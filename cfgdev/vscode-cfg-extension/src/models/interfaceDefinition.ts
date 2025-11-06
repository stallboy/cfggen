import { StructDefinition } from './structDefinition';
import { MetadataDefinition } from './metadataDefinition';
import { NamespaceUtils } from '../utils/namespaceUtils';

export interface InterfaceDefinition {
  name: string;
  namespace: string | null;
  implementations: StructDefinition[];
  metadata: MetadataDefinition[];
  position: Position;
  fullName: string;
}

export interface Position {
  start: { line: number; column: number };
  end: { line: number; column: number };
}

export class InterfaceDefinition {
  constructor(
    name: string,
    namespace: string | null,
    implementations: StructDefinition[] = [],
    metadata: MetadataDefinition[] = [],
    position: Position = { start: { line: 0, column: 0 }, end: { line: 0, column: 0 } }
  ) {
    this.name = name;
    this.namespace = namespace;
    this.implementations = implementations;
    this.metadata = metadata;
    this.position = position;
    this.fullName = NamespaceUtils.buildNamespace(namespace, name);
  }

  static fromAST(astNode: any): InterfaceDefinition {
    const { namespace, name } = NamespaceUtils.parseNamespace(astNode.name);

    const implementations = astNode.implementations?.map((impl: any) => StructDefinition.fromAST(impl)) || [];
    const metadata = astNode.metadata?.map((meta: any) => MetadataDefinition.fromAST(meta)) || [];

    return new InterfaceDefinition(
      name,
      namespace,
      implementations,
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

  addImplementation(struct: StructDefinition): void {
    this.implementations.push(struct);
  }

  removeImplementation(structName: string): void {
    this.implementations = this.implementations.filter(impl => impl.name !== structName);
  }

  getImplementationByName(structName: string): StructDefinition | undefined {
    return this.implementations.find(impl => impl.name === structName);
  }

  getAllFields(): string[] {
    // 返回接口所有实现结构的字段名（去重）
    const allFields = new Set<string>();
    this.implementations.forEach(impl => {
      impl.fields.forEach(field => {
        allFields.add(field.name);
      });
    });
    return Array.from(allFields);
  }

  toString(): string {
    return `interface ${this.fullName}`;
  }
}