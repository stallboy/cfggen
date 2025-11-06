import * as vscode from 'vscode';
import { ConfigFile, ConfigFileBuilder, StructDefinition, InterfaceDefinition, TableDefinition } from '../models';
import { ANTLRParser } from '../parser/antlrParser';
import { CacheService } from './cacheService';
import { Logger } from '../utils/logger';
import { PerformanceMonitor } from '../utils/performance';

export interface FileIndex {
  files: Map<string, ConfigFile>; // URI -> ConfigFile
  structs: Map<string, StructDefinition[]>; // namespace -> structs
  interfaces: Map<string, InterfaceDefinition[]>; // namespace -> interfaces
  tables: Map<string, TableDefinition[]>; // namespace -> tables
  references: Map<string, string[]>; // reference -> file URIs
}

export class FileIndexService {
  private static instance: FileIndexService;
  private index: FileIndex;
  private parser: ANTLRParser;
  private cache: CacheService;
  private logger: Logger;
  private performance: PerformanceMonitor;

  private constructor() {
    this.index = {
      files: new Map(),
      structs: new Map(),
      interfaces: new Map(),
      tables: new Map(),
      references: new Map()
    };
    this.parser = new ANTLRParser();
    this.cache = CacheService.getInstance();
    this.logger = Logger.getInstance();
    this.performance = PerformanceMonitor.getInstance();
  }

  public static getInstance(): FileIndexService {
    if (!FileIndexService.instance) {
      FileIndexService.instance = new FileIndexService();
    }
    return FileIndexService.instance;
  }

  /**
   * 添加或更新文件到索引
   */
  public async addFile(uri: vscode.Uri, content: string): Promise<ConfigFile> {
    const endMeasurement = this.performance.startMeasurement('addFile');

    try {
      // 检查缓存
      const cacheKey = `file:${uri.toString()}`;
      const cachedFile = this.cache.get<ConfigFile>(cacheKey);

      if (cachedFile && cachedFile.content === content) {
        this.logger.debug(`Using cached file: ${uri.toString()}`);
        endMeasurement();
        return cachedFile;
      }

      // 解析文件
      const parseResult = this.parser.parse(content, uri);

      if (!parseResult.success) {
        this.logger.warn(`Parse errors in file ${uri.toString()}:`, parseResult.errors);
      }

      // 构建配置文件对象
      const configFile = ConfigFileBuilder.fromAST(uri, content, parseResult.ast);

      // 更新索引
      this.updateIndex(uri, configFile);

      // 缓存结果
      this.cache.set(cacheKey, configFile, 300000); // 5分钟缓存

      this.logger.debug(`File indexed: ${uri.toString()}`);
      endMeasurement();
      return configFile;
    } catch (error) {
      endMeasurement();
      this.logger.error(`Failed to index file ${uri.toString()}:`, error);
      throw error;
    }
  }

  /**
   * 从索引中移除文件
   */
  public removeFile(uri: vscode.Uri): void {
    const file = this.index.files.get(uri.toString());
    if (!file) {
      return;
    }

    // 从所有索引中移除
    this.removeFromIndex(uri, file);

    // 从文件映射中移除
    this.index.files.delete(uri.toString());

    // 清除缓存
    this.cache.delete(`file:${uri.toString()}`);

    this.logger.debug(`File removed from index: ${uri.toString()}`);
  }

  /**
   * 获取文件
   */
  public getFile(uri: vscode.Uri): ConfigFile | undefined {
    return this.index.files.get(uri.toString());
  }

  /**
   * 查找结构体定义
   */
  public findStruct(structName: string, namespace?: string | null): StructDefinition | undefined {
    const searchNamespaces = namespace ? [namespace] : Array.from(this.index.structs.keys());

    for (const ns of searchNamespaces) {
      const structs = this.index.structs.get(ns) || [];
      const struct = structs.find(s => s.name === structName);
      if (struct) {
        return struct;
      }
    }

    return undefined;
  }

  /**
   * 查找接口定义
   */
  public findInterface(interfaceName: string, namespace?: string | null): InterfaceDefinition | undefined {
    const searchNamespaces = namespace ? [namespace] : Array.from(this.index.interfaces.keys());

    for (const ns of searchNamespaces) {
      const interfaces = this.index.interfaces.get(ns) || [];
      const interfaceDef = interfaces.find(i => i.name === interfaceName);
      if (interfaceDef) {
        return interfaceDef;
      }
    }

    return undefined;
  }

  /**
   * 查找表定义
   */
  public findTable(tableName: string, namespace?: string | null): TableDefinition | undefined {
    const searchNamespaces = namespace ? [namespace] : Array.from(this.index.tables.keys());

    for (const ns of searchNamespaces) {
      const tables = this.index.tables.get(ns) || [];
      const table = tables.find(t => t.name === tableName);
      if (table) {
        return table;
      }
    }

    return undefined;
  }

  /**
   * 查找所有匹配的结构体
   */
  public findAllStructs(pattern?: string): StructDefinition[] {
    const allStructs: StructDefinition[] = [];

    for (const structs of this.index.structs.values()) {
      allStructs.push(...structs);
    }

    if (pattern) {
      return allStructs.filter(struct =>
        struct.name.includes(pattern) || struct.fullName.includes(pattern)
      );
    }

    return allStructs;
  }

  /**
   * 查找所有匹配的接口
   */
  public findAllInterfaces(pattern?: string): InterfaceDefinition[] {
    const allInterfaces: InterfaceDefinition[] = [];

    for (const interfaces of this.index.interfaces.values()) {
      allInterfaces.push(...interfaces);
    }

    if (pattern) {
      return allInterfaces.filter(interfaceDef =>
        interfaceDef.name.includes(pattern) || interfaceDef.fullName.includes(pattern)
      );
    }

    return allInterfaces;
  }

  /**
   * 查找所有匹配的表
   */
  public findAllTables(pattern?: string): TableDefinition[] {
    const allTables: TableDefinition[] = [];

    for (const tables of this.index.tables.values()) {
      allTables.push(...tables);
    }

    if (pattern) {
      return allTables.filter(table =>
        table.name.includes(pattern) || table.fullName.includes(pattern)
      );
    }

    return allTables;
  }

  /**
   * 获取命名空间中的所有定义
   */
  public getNamespaceDefinitions(namespace: string): {
    structs: StructDefinition[];
    interfaces: InterfaceDefinition[];
    tables: TableDefinition[];
  } {
    return {
      structs: this.index.structs.get(namespace) || [],
      interfaces: this.index.interfaces.get(namespace) || [],
      tables: this.index.tables.get(namespace) || []
    };
  }

  /**
   * 查找引用
   */
  public findReferences(reference: string): string[] {
    return this.index.references.get(reference) || [];
  }

  /**
   * 获取索引统计信息
   */
  public getStats(): {
    fileCount: number;
    structCount: number;
    interfaceCount: number;
    tableCount: number;
    referenceCount: number;
  } {
    let structCount = 0;
    let interfaceCount = 0;
    let tableCount = 0;

    for (const structs of this.index.structs.values()) {
      structCount += structs.length;
    }

    for (const interfaces of this.index.interfaces.values()) {
      interfaceCount += interfaces.length;
    }

    for (const tables of this.index.tables.values()) {
      tableCount += tables.length;
    }

    return {
      fileCount: this.index.files.size,
      structCount,
      interfaceCount,
      tableCount,
      referenceCount: this.index.references.size
    };
  }

  /**
   * 清空索引
   */
  public clear(): void {
    this.index = {
      files: new Map(),
      structs: new Map(),
      interfaces: new Map(),
      tables: new Map(),
      references: new Map()
    };
    this.cache.clear();
    this.logger.debug('File index cleared');
  }

  private updateIndex(uri: vscode.Uri, configFile: ConfigFile): void {
    // 首先移除旧版本（如果存在）
    const oldFile = this.index.files.get(uri.toString());
    if (oldFile) {
      this.removeFromIndex(uri, oldFile);
    }

    // 添加新文件
    this.index.files.set(uri.toString(), configFile);

    // 更新结构体索引
    for (const struct of configFile.structs) {
      const namespace = struct.namespace || '';
      if (!this.index.structs.has(namespace)) {
        this.index.structs.set(namespace, []);
      }
      this.index.structs.get(namespace)!.push(struct);
    }

    // 更新接口索引
    for (const interfaceDef of configFile.interfaces) {
      const namespace = interfaceDef.namespace || '';
      if (!this.index.interfaces.has(namespace)) {
        this.index.interfaces.set(namespace, []);
      }
      this.index.interfaces.get(namespace)!.push(interfaceDef);
    }

    // 更新表索引
    for (const table of configFile.tables) {
      const namespace = table.namespace || '';
      if (!this.index.tables.has(namespace)) {
        this.index.tables.set(namespace, []);
      }
      this.index.tables.get(namespace)!.push(table);
    }

    // 更新引用索引
    this.updateReferenceIndex(configFile);
  }

  private removeFromIndex(_uri: vscode.Uri, configFile: ConfigFile): void {
    // 从结构体索引中移除
    for (const struct of configFile.structs) {
      const namespace = struct.namespace || '';
      const structs = this.index.structs.get(namespace);
      if (structs) {
        const index = structs.findIndex(s => s.fullName === struct.fullName);
        if (index !== -1) {
          structs.splice(index, 1);
        }
        if (structs.length === 0) {
          this.index.structs.delete(namespace);
        }
      }
    }

    // 从接口索引中移除
    for (const interfaceDef of configFile.interfaces) {
      const namespace = interfaceDef.namespace || '';
      const interfaces = this.index.interfaces.get(namespace);
      if (interfaces) {
        const index = interfaces.findIndex(i => i.fullName === interfaceDef.fullName);
        if (index !== -1) {
          interfaces.splice(index, 1);
        }
        if (interfaces.length === 0) {
          this.index.interfaces.delete(namespace);
        }
      }
    }

    // 从表索引中移除
    for (const table of configFile.tables) {
      const namespace = table.namespace || '';
      const tables = this.index.tables.get(namespace);
      if (tables) {
        const index = tables.findIndex(t => t.fullName === table.fullName);
        if (index !== -1) {
          tables.splice(index, 1);
        }
        if (tables.length === 0) {
          this.index.tables.delete(namespace);
        }
      }
    }

    // 从引用索引中移除
    this.removeFromReferenceIndex(configFile);
  }

  private updateReferenceIndex(_configFile: ConfigFile): void {
    // 这里可以添加对外键引用、类型引用等的索引
    // 目前先实现基本结构
  }

  private removeFromReferenceIndex(_configFile: ConfigFile): void {
    // 从引用索引中移除相关引用
  }
}