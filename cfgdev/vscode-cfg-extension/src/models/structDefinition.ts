import { FieldDefinition } from './fieldDefinition';
import { ForeignKeyDefinition } from './foreignKeyDefinition';
import { MetadataDefinition } from './metadataDefinition';
import { NamespaceUtils } from '../utils/namespaceUtils';

export interface StructDefinition {
  name: string;
  namespace: string | null;
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

export class StructDefinition {
  constructor(
    name: string,
    namespace: string | null,
    fields: FieldDefinition[] = [],
    foreignKeys: ForeignKeyDefinition[] = [],
    metadata: MetadataDefinition[] = [],
    position: Position = { start: { line: 0, column: 0 }, end: { line: 0, column: 0 } }
  ) {
    this.name = name;
    this.namespace = namespace;
    this.fields = fields;
    this.foreignKeys = foreignKeys;
    this.metadata = metadata;
    this.position = position;
    this.fullName = NamespaceUtils.buildNamespace(namespace, name);
  }

  static fromAST(astNode: any): StructDefinition {
    const { namespace, name } = NamespaceUtils.parseNamespace(astNode.name);

    const fields = astNode.fields?.map((field: any) => FieldDefinition.fromAST(field)) || [];
    const foreignKeys = astNode.foreignKeys?.map((fk: any) => ForeignKeyDefinition.fromAST(fk)) || [];
    const metadata = astNode.metadata?.map((meta: any) => MetadataDefinition.fromAST(meta)) || [];

    return new StructDefinition(
      name,
      namespace,
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
    // 结构体没有显式主键，返回所有字段
    return this.fields;
  }

  toString(): string {
    return `struct ${this.fullName}`;
  }
}