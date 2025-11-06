export interface MetadataDefinition {
  name: string;
  value?: string | number | boolean;
  position: Position;
}

export interface Position {
  start: { line: number; column: number };
  end: { line: number; column: number };
}

export class MetadataDefinition {
  constructor(
    name: string,
    value?: string | number | boolean,
    position: Position = { start: { line: 0, column: 0 }, end: { line: 0, column: 0 } }
  ) {
    this.name = name;
    this.value = value;
    this.position = position;
  }

  static fromAST(astNode: any): MetadataDefinition {
    return new MetadataDefinition(
      astNode.name,
      astNode.value,
      astNode.position
    );
  }

  static create(name: string, value?: string | number | boolean): MetadataDefinition {
    return new MetadataDefinition(name, value);
  }

  hasValue(): boolean {
    return this.value !== undefined;
  }

  getValueAsString(): string | undefined {
    if (this.value === undefined) return undefined;
    return String(this.value);
  }

  getValueAsNumber(): number | undefined {
    if (typeof this.value === 'number') return this.value;
    if (typeof this.value === 'string') {
      const num = parseFloat(this.value);
      return isNaN(num) ? undefined : num;
    }
    return undefined;
  }

  getValueAsBoolean(): boolean | undefined {
    if (typeof this.value === 'boolean') return this.value;
    if (typeof this.value === 'string') {
      return this.value.toLowerCase() === 'true';
    }
    if (typeof this.value === 'number') {
      return this.value !== 0;
    }
    return undefined;
  }

  isNegative(): boolean {
    return this.name.startsWith('-');
  }

  getNormalizedName(): string {
    return this.isNegative() ? this.name.substring(1) : this.name;
  }

  toString(): string {
    if (this.value === undefined) {
      return this.name;
    }
    if (typeof this.value === 'string') {
      return `${this.name} = '${this.value}'`;
    }
    return `${this.name} = ${this.value}`;
  }

  equals(other: MetadataDefinition): boolean {
    return this.name === other.name && this.value === other.value;
  }
}