# AI Follow-up Repair Design

## Context

The AI analysis page currently shows the user's follow-up message immediately, but then rolls it back into the input box. Backend logs show the follow-up answer is often generated and persisted, yet the browser disconnects before the response completes. At the same time, the frontend cannot be rebuilt cleanly because some imported API/page files are missing from the current tree.

## Approved Scope

1. Keep the current AI chat layout and optimistic "message appears immediately + thinking" experience.
2. Stop follow-up questions from rolling back just because the request takes longer than the global frontend timeout.
3. Recover gracefully if the network request is interrupted by refreshing the latest stored messages instead of deleting the optimistic state.
4. Restore missing frontend API/page files required for a successful production build so Docker deployment can complete.

## Design

### Frontend request handling

- Add dedicated AI analysis API helpers with a longer timeout than the global `5s` axios default.
- Keep optimistic user/thinking messages in the conversation while the follow-up request is in flight.
- If the request fails or times out, fetch the latest persisted conversation from the backend before deciding whether to show an error and rehydrate the input.

### Frontend build repairs

- Recreate the missing AI analysis API module used by `AIAnalysis.vue`.
- Recreate the missing crawl-result API module used by the same page.
- Add a lightweight `TaskDetail.vue` page so the router resolves during `vite build`.

### Backend compatibility

- Preserve the current backend follow-up storage flow.
- Only adjust backend code if the frontend repair uncovers a blocking contract mismatch.

## Verification

1. `vite build` succeeds for the frontend.
2. Docker services rebuild and start successfully.
3. Sending a follow-up question no longer immediately rolls back to the input box.
4. If the backend finishes after a slow response, the latest stored answer still appears after the frontend refresh path runs.
