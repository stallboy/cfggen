export interface TypeDefinition {
  baseType: BaseType;
  elementType?: TypeDefinition; // 用于list和map
  keyType?: TypeDefinition; // 用于map
  reference?: string; // 结构体/接口/表引用
  position: Position;
}

export type BaseType = 'bool' | 'int' | 'long' | 'float' | 'str' | 'text' | 'list' | 'map' | 'struct' | 'interface' | 'table';

export interface Position {
  start: { line: number; column: number };
  end: { line: number; column: number };
}

export class TypeDefinition {
  constructor(
    baseType: BaseType,
    elementType?: TypeDefinition,
    keyType?: TypeDefinition,
    reference?: string,
    position: Position = { start: { line: 0, column: 0 }, end: { line: 0, column: 0 } }
  ) {
    this.baseType = baseType;
    this.elementType = elementType;
    this.keyType = keyType;
    this.reference = reference;
    this.position = position;
  }

  static fromAST(astNode: any): TypeDefinition {
    let elementType: TypeDefinition | undefined;
    let keyType: TypeDefinition | undefined;

    if (astNode.elementType) {
      elementType = TypeDefinition.fromAST(astNode.elementType);
    }

    if (astNode.keyType) {
      keyType = TypeDefinition.fromAST(astNode.keyType);
    }

    return new TypeDefinition(
      astNode.baseType,
      elementType,
      keyType,
      astNode.reference,
      astNode.position
    );
  }

  static createPrimitive(baseType: BaseType): TypeDefinition {
    return new TypeDefinition(baseType);
  }

  static createList(elementType: TypeDefinition): TypeDefinition {
    return new TypeDefinition('list', elementType);
  }

  static createMap(keyType: TypeDefinition, valueType: TypeDefinition): TypeDefinition {
    return new TypeDefinition('map', valueType, keyType);
  }

  static createReference(baseType: 'struct' | 'interface' | 'table', reference: string): TypeDefinition {
    return new TypeDefinition(baseType, undefined, undefined, reference);
  }

  isPrimitive(): boolean {
    return ['bool', 'int', 'long', 'float', 'str', 'text'].includes(this.baseType);
  }

  isCollection(): boolean {
    return ['list', 'map'].includes(this.baseType);
  }

  isReference(): boolean {
    return ['struct', 'interface', 'table'].includes(this.baseType);
  }

  isList(): boolean {
    return this.baseType === 'list';
  }

  isMap(): boolean {
    return this.baseType === 'map';
  }

  getElementType(): TypeDefinition | undefined {
    return this.elementType;
  }

  getKeyType(): TypeDefinition | undefined {
    return this.keyType;
  }

  getReference(): string | undefined {
    return this.reference;
  }

  toString(): string {
    switch (this.baseType) {
      case 'list':
        return `list<${this.elementType?.toString() || 'unknown'}>`;
      case 'map':
        return `map<${this.keyType?.toString() || 'unknown'}, ${this.elementType?.toString() || 'unknown'}>`;
      case 'struct':
      case 'interface':
      case 'table':
        return this.reference || this.baseType;
      default:
        return this.baseType;
    }
  }

  equals(other: TypeDefinition): boolean {
    if (this.baseType !== other.baseType) return false;
    if (this.reference !== other.reference) return false;

    if (this.elementType && other.elementType) {
      if (!this.elementType.equals(other.elementType)) return false;
    } else if (this.elementType || other.elementType) {
      return false;
    }

    if (this.keyType && other.keyType) {
      if (!this.keyType.equals(other.keyType)) return false;
    } else if (this.keyType || other.keyType) {
      return false;
    }

    return true;
  }
}