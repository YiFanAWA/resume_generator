This directory contains a bundled open-source CJK font for PDF export:

- `NotoSansSC-VF.ttf`

Bundling a CJK font here makes PDF export stable in local development, JAR
packages, Docker, Linux, macOS, and cloud deployments.

The PDF renderer will automatically try these filenames first:

- NotoSansSC-VF.ttf
- NotoSansSC-Regular.ttf
- NotoSansCJKsc-Regular.ttf
- NotoSansCJK-Regular.ttc
- SourceHanSansSC-Regular.ttf
- SourceHanSansCN-Regular.ttf
- simhei.ttf
- simsun.ttc
- msyh.ttc

Recommended open-source fonts:

- Noto Sans SC
- Noto Sans CJK SC
- Source Han Sans SC

Docker images also install `fonts-noto-cjk` as a system-level fallback. Local
Windows, Linux, and macOS runs try common system CJK font paths if no bundled
font is available.

Do not copy proprietary Windows fonts into a public repository unless the font
license allows redistribution.
