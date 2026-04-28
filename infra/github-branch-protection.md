# GitHub Branch Protection

Recommended protection for `main` after Phase 4 CI is merged.

## Required Status Checks

- `Java services`
- `Frontend`
- `AI service syntax`
- `Build frontend`
- `Build gateway`
- `Build platform-service`
- `Build ai-service`

The container image checks can be required after the first successful `Container Images` workflow run names are confirmed in GitHub.

## Protection Rules

- Require pull request before merging.
- Require at least one approval.
- Require conversation resolution before merging.
- Require status checks to pass before merging.
- Require branches to be up to date before merging.
- Block force pushes.
- Block deletions.
- Restrict direct pushes to maintainers only, or block direct pushes entirely once CI is stable.

## Suggested `gh` Flow

GitHub rulesets differ by plan and organization settings, so prefer configuring this in the repository UI first:

`Settings -> Rules -> Rulesets -> New branch ruleset -> target main`

Only automate this after confirming the exact required check names from the first Phase 4 PR run.
