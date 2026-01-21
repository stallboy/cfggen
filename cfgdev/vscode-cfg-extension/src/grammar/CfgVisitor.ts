
import { AbstractParseTreeVisitor } from "antlr4ng";


import { SchemaContext } from "./CfgParser.js";
import { Schema_eleContext } from "./CfgParser.js";
import { Struct_declContext } from "./CfgParser.js";
import { Interface_declContext } from "./CfgParser.js";
import { Table_declContext } from "./CfgParser.js";
import { Field_declContext } from "./CfgParser.js";
import { Foreign_declContext } from "./CfgParser.js";
import { Key_declContext } from "./CfgParser.js";
import { TypeListContext } from "./CfgParser.js";
import { TypeMapContext } from "./CfgParser.js";
import { TypeBasicContext } from "./CfgParser.js";
import { Type_eleContext } from "./CfgParser.js";
import { RefContext } from "./CfgParser.js";
import { KeyContext } from "./CfgParser.js";
import { MetadataContext } from "./CfgParser.js";
import { Ident_with_opt_single_valueContext } from "./CfgParser.js";
import { Minus_identContext } from "./CfgParser.js";
import { Single_valueContext } from "./CfgParser.js";
import { Ns_identContext } from "./CfgParser.js";
import { IdentifierContext } from "./CfgParser.js";
import { CommentContext } from "./CfgParser.js";


/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by `CfgParser`.
 *
 * @param <Result> The return type of the visit operation. Use `void` for
 * operations with no return type.
 */
export class CfgVisitor<Result> extends AbstractParseTreeVisitor<Result> {
    /**
     * Visit a parse tree produced by `CfgParser.schema`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitSchema?: (ctx: SchemaContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.schema_ele`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitSchema_ele?: (ctx: Schema_eleContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.struct_decl`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitStruct_decl?: (ctx: Struct_declContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.interface_decl`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitInterface_decl?: (ctx: Interface_declContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.table_decl`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitTable_decl?: (ctx: Table_declContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.field_decl`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitField_decl?: (ctx: Field_declContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.foreign_decl`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitForeign_decl?: (ctx: Foreign_declContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.key_decl`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitKey_decl?: (ctx: Key_declContext) => Result;
    /**
     * Visit a parse tree produced by the `TypeList`
     * labeled alternative in `CfgParser.type_`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitTypeList?: (ctx: TypeListContext) => Result;
    /**
     * Visit a parse tree produced by the `TypeMap`
     * labeled alternative in `CfgParser.type_`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitTypeMap?: (ctx: TypeMapContext) => Result;
    /**
     * Visit a parse tree produced by the `TypeBasic`
     * labeled alternative in `CfgParser.type_`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitTypeBasic?: (ctx: TypeBasicContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.type_ele`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitType_ele?: (ctx: Type_eleContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.ref`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitRef?: (ctx: RefContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.key`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitKey?: (ctx: KeyContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.metadata`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitMetadata?: (ctx: MetadataContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.ident_with_opt_single_value`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitIdent_with_opt_single_value?: (ctx: Ident_with_opt_single_valueContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.minus_ident`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitMinus_ident?: (ctx: Minus_identContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.single_value`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitSingle_value?: (ctx: Single_valueContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.ns_ident`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitNs_ident?: (ctx: Ns_identContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.identifier`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitIdentifier?: (ctx: IdentifierContext) => Result;
    /**
     * Visit a parse tree produced by `CfgParser.comment`.
     * @param ctx the parse tree
     * @return the visitor result
     */
    visitComment?: (ctx: CommentContext) => Result;
}

