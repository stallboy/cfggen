import * as vscode from 'vscode';
import { StructDefinition } from './structDefinition';
import { InterfaceDefinition } from './interfaceDefinition';
import { TableDefinition } from './tableDefinition';

export interface ConfigFile {
  uri: vscode.Uri;
  content: string;
  ast: any; // ANTLR4 AST
  structs: StructDefinition[];
  interfaces: InterfaceDefinition[];
  tables: TableDefinition[];
  parseErrors: ParseError[];
  lastModified: number;
}

export interface ParseError {
  line: number;
  column: number;
  message: string;
  severity: 'error' | 'warning';
}

export class ConfigFileBuilder {
  private uri: vscode.Uri;
  private content: string = '';
  private ast: any = null;
  private structs: StructDefinition[] = [];
  private interfaces: InterfaceDefinition[] = [];
  private tables: TableDefinition[] = [];
  private parseErrors: ParseError[] = [];
  private lastModified: number = Date.now();

  constructor(uri: vscode.Uri) {
    this.uri = uri;
  }

  setContent(content: string): ConfigFileBuilder {
    this.content = content;
    this.lastModified = Date.now();
    return this;
  }

  setAst(ast: any): ConfigFileBuilder {
    this.ast = ast;
    return this;
  }

  addStruct(struct: StructDefinition): ConfigFileBuilder {
    this.structs.push(struct);
    return this;
  }

  addInterface(interfaceDef: InterfaceDefinition): ConfigFileBuilder {
    this.interfaces.push(interfaceDef);
    return this;
  }

  addTable(table: TableDefinition): ConfigFileBuilder {
    this.tables.push(table);
    return this;
  }

  addParseError(error: ParseError): ConfigFileBuilder {
    this.parseErrors.push(error);
    return this;
  }

  build(): ConfigFile {
    return {
      uri: this.uri,
      content: this.content,
      ast: this.ast,
      structs: this.structs,
      interfaces: this.interfaces,
      tables: this.tables,
      parseErrors: this.parseErrors,
      lastModified: this.lastModified
    };
  }

  static fromAST(uri: vscode.Uri, content: string, ast: any): ConfigFile {
    const builder = new ConfigFileBuilder(uri)
      .setContent(content)
      .setAst(ast);

    // 从AST中提取结构体、接口和表
    if (ast && ast.elements) {
      for (const element of ast.elements) {
        switch (element.type) {
          case 'struct':
            builder.addStruct(StructDefinition.fromAST(element));
            break;
          case 'interface':
            builder.addInterface(InterfaceDefinition.fromAST(element));
            break;
          case 'table':
            builder.addTable(TableDefinition.fromAST(element));
            break;
        }
      }
    }

    return builder.build();
  }

  static createEmpty(uri: vscode.Uri): ConfigFile {
    return new ConfigFileBuilder(uri).build();
  }
}