# STATUS

Newest entry first. Add new entries at the top using Edit only.

## 2026-11-23 15:02 (Cairo) - Phase 0: infrastructure ready, sources unreachable from cloud session

Done:
- Public repos: ahmedhatemamh/mawaqit-app (empty), ahmedhatemamh/mawaqit-data (README + docs/index.html + docs/CNAME).
- GitHub Pages on mawaqit-data (main, /docs), custom domain data.ahmedhatem.work (DNS check pending).
- CodeRabbit installed on both repos.
- Cloudflare CNAME data -> ahmedhatemamh.github.io (DNS only).
- Package name decided: work.ahmedhatem.mawaqit. App name: Mawaqit Misr.

Findings:
- esa.gov.eg and dar-alifta.org return 403 from the cloud session proxy (CONNECT tunnel failed), so page structure is unknown.

Pending (needs Ahmed's approval):
- Where the scraper runs and how we inspect the source pages.
- Verify https://data.ahmedhatem.work opens and Enforce HTTPS is on.
- Pending "Permission updates requested" on CodeRabbit and Claude apps (untouched on purpose).
