/*!
 * Lunr languages, `Chinese` language - MODIFIED FOR JIEBA-WASM
 * Original: https://github.com/MihaiValentin/lunr-languages
 * This version expects an initialized jieba-wasm module's API to be passed.
 *
 * Original Copyright 2019, Felix Lian (repairearth)
 * http://www.mozilla.org/MPL/
 */
;(function(root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as a module that exports a setup function.
    define(['lunr'], function (lunr) {
      // The module exports a function that takes the jiebaWasmApi
      // and then calls the original factory.
      return function(jiebaWasmApi) {
        return factory(lunr, jiebaWasmApi);
      };
    });
  } else if (typeof exports === 'object' && typeof module !== 'undefined' && module.exports) {
    // Node.js-like environments / CommonJS. Export a setup function.
    // User would typically:
    // const lunr = require('lunr');
    // const { init, cut } = require('jieba-wasm/pkg/nodejs/jieba_rs_wasm.js'); // Or web version if appropriate
    // await init(); // Or however jieba-wasm for Node is initialized
    // const setupLunrZh = require('./your-modified-lunr.zh.js');
    // setupLunrZh(lunr, { cut });
    module.exports = function(lunr, jiebaWasmApi) {
      if (typeof lunr === 'undefined' || !lunr.version) {
        try {
          lunr = require('lunr');
        } catch (e) {
          throw new Error('Lunr is not available. Please pass it as an argument or ensure it is requireable.');
        }
      }
      return factory(lunr, jiebaWasmApi);
    };
  } else {
    // Browser globals.
    // User should load lunr.js, then load and initialize jieba-wasm,
    // then call this setup function.
    // e.g.:
    // <script src="lunr.js"></script>
    // <script type="module">
    //   import init, { cut } from 'https://cdn.jsdelivr.net/npm/jieba-wasm@latest_version/pkg/web/jieba_rs_wasm.js';
    //   await init();
    //   window.jiebaWasmApi = { cut }; // Expose necessary functions
    // </script>
    // <script src="path/to/this/modified-lunr.zh.js"></script> // <script>
    //   window.setupLunrZh(lunr, window.jiebaWasmApi); // Call the setup function
    // </script>
    if (!root.lunr) {
        throw new Error('Lunr is not loaded. Please load Lunr.js before this script.');
    }
    root.setupLunrZh = function(lunrInstance, jiebaWasmApi) {
      factory(lunrInstance || root.lunr, jiebaWasmApi);
    };
  }
}(this, function(lunr, jiebaWasmApi) {

  if (typeof lunr === 'undefined' || !lunr.version) {
    throw new Error('Lunr is not present. Please include/require Lunr or pass it to the setup function.');
  }
  if (typeof lunr.stemmerSupport === 'undefined') {
    throw new Error('Lunr stemmer support is not present. Please include/require Lunr stemmer support.');
  }
  if (!jiebaWasmApi || typeof jiebaWasmApi.cut !== 'function') {
    throw new Error('An initialized jieba-wasm API object with a "cut" function must be provided.');
  }

  var isLunr2 = lunr.version[0] == "2";

  /**
   * lunr.zh is a constructor for a new language specific pipeline.
   *
   * @constructor
   */
  lunr.zh = function() {
    this.pipeline.reset();
    this.pipeline.add(
      lunr.zh.trimmer,
      lunr.zh.stopWordFilter,
      lunr.zh.stemmer // Chinese stemmer is a TODO in the original, remains so.
    );

    // Lunr 2.x has a tokenizer property in the constructor
    // a backwards compatible check for old versions of lunr is not necessary here
    if (isLunr2) {
      this.tokenizer = lunr.zh.tokenizer;
    } else {
      // Older lunr versions (0.6.x, 0.7.x, 1.x) might have different tokenizer properties
      if (lunr.tokenizer) { // For lunr version 0.6.0
        lunr.tokenizer = lunr.zh.tokenizer;
      }
      if (this.tokenizerFn) { // For lunr version 0.7.0 -> 1.0.0
        this.tokenizerFn = lunr.zh.tokenizer;
      }
    }
  };

  /**
   * lunr.zh.tokenizer is a function that segments Chinese text into tokens.
   * It uses the provided jiebaWasmApi.cut method.
   *
   * @param {?(string|object|object[])} obj The object to convert into tokens
   * @returns {lunr.Token[]}
   */
  lunr.zh.tokenizer = function(obj) {
    if (obj == null || obj === undefined) {
      return [];
    }

    if (Array.isArray(obj)) {
      return obj.map(function(t) {
        var tokenStr = t.toString().toLowerCase();
        // For Lunr 2.x, tokens can carry metadata.
        // If t is already a Token, we might want to preserve metadata,
        // but typically tokenizer receives raw strings from array.
        return isLunr2 ? new lunr.Token(tokenStr) : tokenStr;
      });
    }

    var str = obj.toString().trim().toLowerCase();
    if (str === '') {
      return [];
    }

    // Use jiebaWasmApi.cut. The original file used HMM mode (true).
    var segments = jiebaWasmApi.cut(str, true);
    var tokens = [];

    // jieba-wasm's `cut` directly returns an array of word segments.
    // No need for `seg.split(' ')` which was in the original for nodejieba.
    segments.forEach(function(seg) {
      if (seg && seg.trim() !== '') { // Ensure not empty string
         tokens.push(seg);
      }
    });

    // Filter out any possible remaining empty tokens (should be rare with jieba-wasm)
    tokens = tokens.filter(function(token) {
        return !!token;
    });

    if (!isLunr2) {
      return tokens; // For older Lunr versions, just return array of strings
    }

    // For Lunr 2.x, map to lunr.Token objects with metadata
    var fromIndex = 0; // Used to track position in the original string for metadata
    return tokens.map(function(token, i) { // Use 'i' for the index in the tokens array
      var start = str.indexOf(token, fromIndex);
      var tokenLength = token.length;

      if (start === -1) {
        // Fallback: if token not found (e.g. due to normalization or complex cases).
        // This might happen if jieba normalizes the token in a way that str.indexOf can't find it.
        // A more robust solution would require jieba to return offsets, but this is a common simple approach.
        // For now, we'll try to find it anywhere if not found from fromIndex
        var searchStart = str.indexOf(token);
        if (searchStart !== -1) {
            start = searchStart;
        } else {
            // If still not found, assign a default or log a warning.
            // This could mean the tokenization is not perfectly aligned with simple string searching.
            // console.warn(`Token "${token}" not found in original string for position metadata.`);
            start = 0; // Default to start, or consider how to handle this.
        }
      }

      var tokenMetadata = {};
      // `position` metadata format is [startPosition, tokenLength]
      tokenMetadata["position"] = [start, tokenLength];
      // `index` metadata is the index of this token in the array of tokens produced by the tokenizer
      tokenMetadata["index"] = i;


      // Update fromIndex for the next token search.
      // This simple update works if tokens are mostly sequential and non-overlapping.
      fromIndex = start + tokenLength;

      return new lunr.Token(token, tokenMetadata);
    });
  };

  /* lunr trimmer function */
  lunr.zh.wordCharacters = "\\w\\u4e00-\\u9fa5"; // Same as original
  lunr.zh.trimmer = lunr.trimmerSupport.generateTrimmer(lunr.zh.wordCharacters);
  lunr.Pipeline.registerFunction(lunr.zh.trimmer, 'trimmer-zh');

  /* lunr stemmer function */
  lunr.zh.stemmer = (function() {
    /* TODO Chinese stemmer  */
    return function(word) {
      return word;
    };
  })();
  lunr.Pipeline.registerFunction(lunr.zh.stemmer, 'stemmer-zh');

  /* lunr stop word filter. */
  lunr.zh.stopWordFilter = lunr.generateStopWordFilter(
    '的 一 不 在 人 有 是 为 為 以 于 於 上 他 而 后 後 之 来 來 及 了 因 下 可 到 由 这 這 与 與 也 此 但 并 並 个 個 其 已 无 無 小 我 们 們 起 最 再 今 去 好 只 又 或 很 亦 某 把 那 你 乃 它 吧 被 比 别 趁 当 當 从 從 得 打 凡 儿 兒 尔 爾 该 該 各 给 給 跟 和 何 还 還 即 几 幾 既 看 据 據 距 靠 啦 另 么 麽 每 嘛 拿 哪 您 凭 憑 且 却 卻 让 讓 仍 啥 如 若 使 谁 誰 虽 隨 同 所 她 哇 嗡 往 些 向 沿 哟 喲 用 咱 则 則 怎 曾 至 致 着 著 诸 諸 自'.split(' ')
  );
  lunr.Pipeline.registerFunction(lunr.zh.stopWordFilter, 'stopWordFilter-zh');

  // No need to return anything, as the factory modifies the passed lunr object directly
  // or sets up `lunr.zh` which is then used to register the language.
}));