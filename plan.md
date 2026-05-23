1. **Add detailed timing logs to `T9Engine.generateCandidates()`**:
   - Record the sizes of buffers, compositions, and SQLite queries.
   - Calculate elapsed time per method run.
2. **Short-circuit `getSentenceCandidates()`**:
   - If exact phrases are already sufficient, return them and short-circuit dynamic concatenation loops.
   - Limit the intermediate DP list sizes, e.g. DP list capped at 5 or 10.
   - Keep track of queries to avoid duplicate lookups within `generateCandidates`.
3. **Add dictionary query caching in `T9Engine`**:
   - Limit repetitive cache calls for the same pinyin sequences. Cache batch results on a per-request basis or short-lived memory.
4. **Optimize `CandidateViewController.refreshFromState()`**:
   - Diff the candidates list before `removeAllViews()`.
   - Update `TextView` components dynamically instead of fully recreating them when text changes. Create a view pool for candidates.
   - Only call `removeAllViews` if completely necessary or when visibility turns off/on.
5. **Caching in `T9Engine.getCandidates()`**:
   - Cache results from identical buffer strings to avoid repeating calculations when backspacing to a previous state or refreshing.
6. **Pre-commit checks and tests**.
