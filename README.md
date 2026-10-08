# <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/chaka_hippo_96x72.png" width="72px"> Chaka Book Reader

An Android reader app committed to improving reading experience.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/net.timelegend.chaka.viewer.app/)
[<img src="https://raw.githubusercontent.com/vadret/android/master/assets/get-github.png" alt="Get it on GitHub" height="80">](https://github.com/elementdavv/chaka/releases)

[![Downloads last month](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fgithub.com%2Fkitswas%2Ffdroid-metrics-dashboard%2Fraw%2Frefs%2Fheads%2Fmain%2Fprocessed%2Fmonthly%2Fnet.timelegend.chaka.viewer.app.json&query=%24.total_downloads&logo=fdroid&label=Downloads%20last%20month)](https://f-droid.org/packages/net.timelegend.chaka.viewer.app/)
[![Total Downloads](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fgithub.com%2Fkitswas%2Ffdroid-metrics-dashboard%2Fraw%2Frefs%2Fheads%2Fmain%2Fprocessed%2Ftotal%2Fnet.timelegend.chaka.viewer.app.json&query=%24.total_downloads&logo=fdroid&label=Total%20Downloads)](https://f-droid.org/packages/net.timelegend.chaka.viewer.app/)
[![Github latest releases](https://img.shields.io/github/downloads/elementdavv/chaka/latest/total.svg?logo=github&label=Latest%20Downloads&color=darkgreen)](https://GitHub.com/elementdavv/chaka/releases/latest)
[![Github all releases](https://img.shields.io/github/downloads/elementdavv/chaka/total.svg?logo=github&label=Total%20Downloads&color=darkgreen)](https://GitHub.com/elementdavv/chaka/releases/)

## Supported Files:

- Documents: PDF, EPUB, DJVU, MOBI, FB2, XPS, TXT, HTML
- Comics: CBZ, CBR, CBT
- Office files: DOCX, XLSX, PPTX
- Multi-Page images: TIFF
- Archives: ZIP, GZIP, RAR, TAR, 7-ZIP packages of above files

## Features

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/flip_vertical.png"> Flip Vertical

  Both **Flip Vertical** and **Flip Horizontal** modes are supported.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/text_left.png"> RtL Text

  In top-to-bottom, right-to-left script (TB-RL or vertical), writing starts from the top of the page and continues to the bottom, proceeding from right to left for new lines, pages numbered from right to left (from Wikipedia). The **RtL Text** mode can be applied to books of East Asian languages, including classical Chinese, Japanese and Korean.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/single_column.png"> Single Column

  Some PDF books were scanned in a way that left and right pages were put in one image, resulting in a so called dual-spread page. In the scenario, **Single Column** mode plays a role. It splits a dual-spread page into two pages.

  **Single Column** mode can also be a conveniency for magazines and scientific papers with two columns in a page.

  In **Single Column** mode, all pages except first and last page are splitted.

- Continuous scroll

  **Continuous scroll** has been perfectly implemented in all scenarios.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/fit_screen.png"> Fit Screen

  In **Flip Horizontal and RtL Text** mode, or **Flip Vertical and Not RtL Text** mode, default minimum scale of non reflowable documents(eg PDFs) is increased to fit the page height or width to the window which is the best reading practise. But if the page size exceeds the window, this function may be used.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/lock.png"> Lock Stray

  When flinging or scrolling a zoomed page, it can hardly move in straight horizontal/vertical direction, and be annoying reading experience. Here the **Lock Stray** mode will make a help.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/crop_margin.png"> Crop Margin

  Crop page margins to get more efficient reading space. All document types are supported.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/focus.png"> Focus

  **Focus** mode will keep page position across zoomed pages. On moving to a definite page, that is, tapping to next/prev page, choosing on Toc/bookmark table, navigating through links or text search, and skimming on page slider, it will present visible content area of new page in same position as the old one. Note that scroll/fling operation is an exception.

  On entering **Focus** mode, current page will zoom automatically to match screen in shorter dimension and center itself.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/smart_focus.png"> Smart Focus

  With scanned PDF books, content area scarcely appear exactly centered in a page. More probable it inclines toward left or right side. **Smart Focus** deals with the scenario. By adjusting the position of even or odd pages accordingly, it makes **Focus** mode behave smartly.

  **Smart Focus** must work with **Focus** mode to make sense.

- Text Select

  **Text Select** toolbar is implemented, along with select point magnifier. Operations of copy, share, translate, and more are supported.

  During text selecting, page navigation operations still work as ever.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/color.png"> Color Palette

  **Color Palette** are for maxmium legibility and are ideal for reducing eye strain conductive to focused reading.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/format.png"> Font Size

  **Font Size** function works in flowable documents, like EPUBs.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/option.png"> Options

  Document Options are applied to current document. Global Options are applied to all documents. Document Options priorizes over Global Options.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/contents.png"> Contents

  **Contents** menu includes following two functions:

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/toc.png"> Table of Contents

  **Table of Contents** will show up if the document has one. It supports multi-level headings, heading collapsing and expanding. It always keep sync with current page.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/bookmark.png"> Bookmarks

  **Bookmarks** works across reading sessions. Double tap on a page to create a bookmark.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/link.png"> Activate Links

  **Activate Links** and make them navigable.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/search.png"> Search

  Full text **Search** and navigate through search results.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/landscape.png"> Landscape

  Change to **Landscape** view manually.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/share.png"> Share Book

  **Share** current book to Contacts or other apps.

- Scrollable Toobar

  Scrollable **Toolbar** can accommodate more buttons for extended funtions. To avoid overlapped with status bar, the **Toolbar** can be moved to bottom.

- Pros and Cons

  In case of big books(thousands of pages), PDFs were opened very quick, and EPUBs badly slow.

## Introduction in Youtube

[![Chaka Book Reader](https://img.youtube.com/vi/KkB2vlDj_6g/0.jpg)](https://www.youtube.com/watch?v=KkB2vlDj_6g)

## Usage tips

- Launch Chaka, from file picker choose a file to open. Or, launch your favorite file manager, open a file with Chaka.
- Function buttons will show up in **Toolbar** when the corresponding functions are applicable.
- Long press on a **Toolbar** button, to show its function tooltip.
- Double tap the book title on **Toolbar** to close Chaka immediatly.
- Tap in left/top/right/bottom side, to move forward/backward one page.
- Tap in middle, to show/hide **Toolbar** and **Page Indicator**
- Tap and move to scrol view
- Pinch to zoom in/out view
- Fling to **Scroll Continuously**. Under **Lock Stray** mode, a zoomed page will scroll in straight direction.
- Under the combination of **Flip Horizontal and not Rtl Text** mode, or of **Flip Vertical and Rtl Text** mode, and scroll to where between two pages, it will slide slowly into the near page. This behavior guarantees that any page contents will not be cut off.
- Under the combination of **Flip Horizontal and Rtl Text** mode, or of **Flip Vertical and not Rtl Text** mode, pages can stay at any position which will never cut off page contents. This behavior makes reading across two pages comfortably.
- Long press on text, to begin **text select** operation.
- Double tap on page to create a **Bookmark**.
- In **Contents** window, swipe left/right to switch **Contents** view or close window.
- In **Help** window, swipe right to close window.
- All reading states as of page scale, position, last read page, as well as all enable button states are remembered across reading sessions for per book.
- In general, to get the best reading experience mutiple function modes can be employed, adding appropriate screen orientation if needed.

## Credits

- [MuPDF Android Viewer](https://github.com/ArtifexSoftware/mupdf-android-viewer) and developers
- [MarkedView](https://github.com/mittsu333/MarkedView-for-Android) for help document rendering

## Contacts

- GitHub repo: [https://github.com/elementdavv/chaka](https://github.com/elementdavv/chaka)
- Email: elementdavv@hotmail.com
- Telegram: [@elementdavv](https://t.me/elementdavv)
- X(Twitter): [@elementdavv](https://x.com/elementdavv)

## Support

If you enjoy Chaka, consider supporting or hiring the maintainer [@elementdavv](https://x.com/elementdavv) [![donate](https://raw.githubusercontent.com/elementdavv/chaka/master/resources/paypal-logo.png)](https://paypal.me/timelegend)
