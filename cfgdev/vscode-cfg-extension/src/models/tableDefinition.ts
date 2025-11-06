import { FieldDefinition } from './fieldDefinition';
import { ForeignKeyDefinition } from './foreignKeyDefinition';
import { MetadataDefinition } from './metadataDefinition';
import { NamespaceUtils } from '../utils/namespaceUtils';

export interface TableDefinition {
  name: string;
  namespace: string | null;
  primaryKey: string[];
  fields: FieldDefinition[];
  foreignKeys: ForeignKeyDefinition[];
  metadata: MetadataDefinition[];
  position: Position;
  fullName: string;
}

export interface Position {
  start: { line: number; column: number };
  end: { line: number; column: number };
}

export class TableDefinition {
  constructor(
    name: string,
    namespace: string | null,
    primaryKey: string[] = [],
    fields: FieldDefinition[] = [],
    foreignKeys: ForeignKeyDefinition[] = [],
    metadata: MetadataDefinition[] = [],
    position: Position = { start: { line: 0, column: 0 }, end: { line: 0, column: 0 } }
  ) {
    this.name = name;
    this.namespace = namespace;
    this.primaryKey = primaryKey;
    this.fields = fields;
    this.foreignKeys = foreignKeys;
    this.metadata = metadata;
    this.position = position;
    this.fullName = NamespaceUtils.buildNamespace(namespace, name);
  }

  static fromAST(astNode: any): TableDefinition {
    const { namespace, name } = NamespaceUtils.parseNamespace(astNode.name);

    const primaryKey = astNode.primaryKey || [];
    const fields = astNode.fields?.map((field: any) => FieldDefinition.fromAST(field)) || [];
    const foreignKeys = astNode.foreignKeys?.map((fk: any) => ForeignKeyDefinition.fromAST(fk)) || [];
    const metadata = astNode.metadata?.map((meta: any) => MetadataDefinition.fromAST(meta)) || [];

    return new TableDefinition(
      name,
      namespace,
      primaryKey,
      fields,
      foreignKeys,
      metadata,
      astNode.position
    );
  }

  getFieldByName(fieldName: string): FieldDefinition | undefined {
    return this.fields.find(field => field.name === fieldName);
  }

  getForeignKeyByField(fieldName: string): ForeignKeyDefinition | undefined {
    return this.foreignKeys.find(fk => fk.fieldName === fieldName);
  }

  getMetadataValue(metadataName: string): string | number | boolean | undefined {
    const metadata = this.metadata.find(meta => meta.name === metadataName);
    return metadata?.value;
  }

  hasMetadata(metadataName: string): boolean {
    return this.metadata.some(meta => meta.name === metadataName);
  }

  getPrimaryKeyFields(): FieldDefinition[] {
    return this.fields.filter(field => this.primaryKey.includes(field.name));
  }

  isPrimaryKeyField(fieldName: string): boolean {
    return this.primaryKey.includes(fieldName);
  }

  validatePrimaryKey(): boolean {
    // 验证主键字段是否存在
    return this.primaryKey.every(pk => this.fields.some(field => field.name === pk));
  }

  toString(): string {
    return `table ${this.fullName}`;
  }
}