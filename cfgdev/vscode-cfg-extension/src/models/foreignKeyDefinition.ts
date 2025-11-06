export interface ForeignKeyDefinition {
  fieldName: string;
  referenceType: ReferenceType;
  targetTable: string;
  targetKey: string[];
  isNullable: boolean;
  position: Position;
}

export type ReferenceType = 'single' | 'list';

export interface Position {
  start: { line: number; column: number };
  end: { line: number; column: number };
}

export class ForeignKeyDefinition {
  constructor(
    fieldName: string,
    referenceType: ReferenceType,
    targetTable: string,
    targetKey: string[] = [],
    isNullable: boolean = false,
    position: Position = { start: { line: 0, column: 0 }, end: { line: 0, column: 0 } }
  ) {
    this.fieldName = fieldName;
    this.referenceType = referenceType;
    this.targetTable = targetTable;
    this.targetKey = targetKey;
    this.isNullable = isNullable;
    this.position = position;
  }

  static fromAST(astNode: any): ForeignKeyDefinition {
    return new ForeignKeyDefinition(
      astNode.fieldName || 'unknown',
      astNode.referenceType,
      astNode.targetTable,
      astNode.targetKey || [],
      astNode.isNullable || false,
      astNode.position
    );
  }

  isSingleReference(): boolean {
    return this.referenceType === 'single';
  }

  isListReference(): boolean {
    return this.referenceType === 'list';
  }

  getTargetKeyString(): string {
    return this.targetKey.join('.');
  }

  toString(): string {
    const refSymbol = this.isSingleReference() ? '->' : '=>';
    const nullable = this.isNullable ? '?' : '';
    return `${this.fieldName}${nullable} ${refSymbol} ${this.targetTable}.${this.getTargetKeyString()}`;
  }

  equals(other: ForeignKeyDefinition): boolean {
    return (
      this.fieldName === other.fieldName &&
      this.referenceType === other.referenceType &&
      this.targetTable === other.targetTable &&
      JSON.stringify(this.targetKey) === JSON.stringify(other.targetKey) &&
      this.isNullable === other.isNullable
    );
  }
}