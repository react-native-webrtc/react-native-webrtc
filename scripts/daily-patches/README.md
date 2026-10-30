# Fork maintenance

The Daily changes on top of upstream [react-native-webrtc](https://github.com/react-native-webrtc/react-native-webrtc) live in this folder as a patch queue:

- [`manifest.json`](manifest.json) explains each patch: why it exists, when it becomes obsolete, and how to verify it.
- [`patches/`](patches) holds the patches themselves.

Two Claude Code skills (in `.claude/skills/` at the repo root) cover the routine work.

## Updating to a new upstream release

In Claude Code, run `/sync-upstream`, or ask e.g. "sync with upstream 125.0.0". The skill:

1. Checks how each patch applies to the new release, and agrees on a plan with you.
2. Replays the patches on the new tag (branch `daily/<tag>`), resolving conflicts there.
3. Merges upstream into `main-synced-to-<tag>` (and creates `main-pre-syncing-to-<tag>` from `origin/main`), sets the version to `<tag>-daily.1`, regenerates the patches and updates the manifest.
4. Verifies the result: the branch equals the patched tag, no upstream change was lost, lint/build pass, and the self-test below passes.

It asks before creating the merge commit, and never pushes. Pushing the branches, opening the PR and publishing are up to you. react-native-daily-js also needs its `@daily-co/react-native-webrtc` version range widened before apps can install the new release.

## Testing the fork

In Claude Code, run `/daily-selftest`, or ask "run the self-test". The skill installs the current checkout into [`examples/DailySelfTest`](../../examples/DailySelfTest), builds a release build, and runs the automated checks on an Android emulator or iOS simulator. If any check fails, it tells you which patch is involved. You need an emulator or simulator running, and `npm install` done in the repo root.

The same steps without Claude, from the repo root:

```bash
cd examples/DailySelfTest
npm run install-local
npx expo prebuild --platform android --no-install
npm run android
npm test -- android        # or ios; exits 1 unless failed=0
```

Run the self-test after any change to the fork, not only after a sync. Fork changes must also go into the patch queue (see "Changing the fork between syncs" in `.claude/skills/sync-upstream/SKILL.md`), otherwise the next sync drops them.

## Without Claude

[`patches.sh`](patches.sh) does the mechanical parts. Run it without arguments for usage:

- `check <tag>`: how each patch applies to `<tag>`.
- `generate`: rebuild `patches/` from `daily/<baseTag>`.
- `verify <tag> <ref>` / `audit <base> <new> <ref>`: `<ref>` equals the patched tag, and nothing from upstream was lost.
- `build`: lint, bob build, and the `index.d.ts` checks.
