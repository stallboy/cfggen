import { TypeDefinition } from './typeDefinition';
import { ForeignKeyDefinition } from './foreignKeyDefinition';
import { MetadataDefinition } from './metadataDefinition';

export interface FieldDefinition {
  name: string;
  fieldType: TypeDefinition;
  foreignKey?: ForeignKeyDefinition;
  metadata: MetadataDefinition[];
  position: Position;
}

export interface Position {
  start: { line: number; column: number };
  end: { line: number; column: number };
}

export class FieldDefinition {
  constructor(
    name: string,
    fieldType: TypeDefinition,
    foreignKey?: ForeignKeyDefinition,
    metadata: MetadataDefinition[] = [],
    position: Position = { start: { line: 0, column: 0 }, end: { line: 0, column: 0 } }
  ) {
    this.name = name;
    this.fieldType = fieldType;
    this.foreignKey = foreignKey;
    this.metadata = metadata;
    this.position = position;
  }

  static fromAST(astNode: any): FieldDefinition {
    const fieldType = TypeDefinition.fromAST(astNode.fieldType);
    const foreignKey = astNode.foreignKey ? ForeignKeyDefinition.fromAST(astNode.foreignKey) : undefined;
    const metadata = astNode.metadata?.map((meta: any) => MetadataDefinition.fromAST(meta)) || [];

    return new FieldDefinition(
      astNode.name,
      fieldType,
      foreignKey,
      metadata,
      astNode.position
    );
  }

  getMetadataValue(metadataName: string): string | number | boolean | undefined {
    const metadata = this.metadata.find(meta => meta.name === metadataName);
    return metadata?.value;
  }

  hasMetadata(metadataName: string): boolean {
    return this.metadata.some(meta => meta.name === metadataName);
  }

  isNullable(): boolean {
    // 检查是否有nullable元数据
    return this.hasMetadata('nullable') || this.getMetadataValue('nullable') === true;
  }

  isRequired(): boolean {
    // 检查是否有required元数据
    return this.hasMetadata('required') || this.getMetadataValue('required') === true;
  }

  isForeignKey(): boolean {
    return !!this.foreignKey;
  }

  getForeignKeyTarget(): string | undefined {
    return this.foreignKey?.targetTable;
  }

  toString(): string {
    return `${this.name}: ${this.fieldType.toString()}`;
  }
}