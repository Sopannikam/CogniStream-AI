# Commit dates and verification

The nine historical commits use March 1–June 15, 2026 timestamps assigned at the repository owner's request. These are adjusted Git metadata, not a record of development activity during those months.

The original commits were dated July 17–18 and October 12, 2026. GitHub records this repository as created on July 17, 2026. The backend tests, frontend tests, and production builds documented in [Verification](VERIFICATION.md) ran on October 12, 2026.

## Historical commits

| Adjusted date | Original date | Change |
| --- | --- | --- |
| March 1, 2026 | July 17, 2026 | Initial commit |
| March 3, 2026 | July 17, 2026 | Spring Boot dependencies |
| March 7, 2026 | July 17, 2026 | Merge of initial project setup |
| March 14, 2026 | July 18, 2026 | Project foundation and packages |
| April 4, 2026 | October 12, 2026 | GitHub user entity and repository |
| May 9, 2026 | October 12, 2026 | OAuth, indexing, frontend, and streaming chat |
| May 16, 2026 | October 12, 2026 | Record the first passing CI run |
| June 7, 2026 | October 12, 2026 | Source and documentation cleanup |
| June 15, 2026 | October 12, 2026 | Record checks after cleanup |

The rewrite preserved each commit's code tree, message, identities, and parent relationships. Commit IDs changed. The initial commit's signature was removed from its rewritten version because changing the metadata invalidates that signature. A local Git bundle preserves the original history.

## Reading the CI evidence

The linked GitHub Actions runs refer to the original commit IDs:

- [Initial CI run](https://github.com/Sopannikam/CogniStream-AI/actions/runs/38273222323): original commit `1eef546`, rewritten as `4d61f3d`.
- [Cleanup CI run](https://github.com/Sopannikam/CogniStream-AI/actions/runs/38288344020): original commit `d431421`, rewritten as `0b53a17`.

Those original and rewritten commits have identical source trees. The runs establish what passed in October; the adjusted Git dates do not move the runs into May or June.

This documentation correction and subsequent work use their actual commit dates. The adjusted March–June range applies only to the nine historical commits above.
