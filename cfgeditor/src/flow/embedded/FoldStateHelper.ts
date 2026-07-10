import { Folds } from './Folds';
import { JSONObject } from '../../api/recordModel';

/**
 * Fold状态管理助手 - 统一fold状态获取逻辑
 *
 * 提供统一的fold状态管理，消除代码重复
 */
export class FoldStateHelper {
  /**
   * 获取fold状态（优先本地状态，其次对象字段）
   * @param folds 本地fold状态
   * @param fieldChain 字段路径
   * @param obj 数据对象（可选）
   * @returns fold状态，undefined表示未设置
   */
  static getFoldState(
    folds: Folds,
    fieldChain: (string | number)[],
    obj?: JSONObject
  ): boolean | undefined {
    // 优先使用本地状态
    const localFold = folds.isFold(fieldChain);
    if (localFold !== undefined) {
      return localFold;
    }

    // 其次使用对象的$fold字段
    if (obj) {
      return obj['$fold'] as boolean | undefined;
    }

    // 默认undefined
    return undefined;
  }

  /**
   * 检查是否应该内嵌（折叠状态）
   * @param folds 本地fold状态
   * @param fieldChain 字段路径
   * @param obj 数据对象
   * @returns true表示应该内嵌，false表示应该展开
   */
  static shouldEmbed(
    folds: Folds,
    fieldChain: (string | number)[],
    obj: JSONObject
  ): boolean {
    const foldState = this.getFoldState(folds, fieldChain, obj);
    return foldState !== false; // true或undefined都内嵌
  }
}
