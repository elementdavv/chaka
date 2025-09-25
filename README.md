## Chaka Book Reader
An Android reader app committed to improving reading experience.

PDF, EPUB, MOBI, CBZ, FB2, XPS, TXT, HTML, OFFICE(DOCX,XLSX,PPTX) and ZIP/GZIP are supported.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/net.timelegend.chaka.viewer.app/)

Or download the latest APK from the [Releases Section](https://github.com/elementdavv/chaka/releases/latest).

### Features
- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/flip_vertical.png"> Flip Vertical

  **Flip Vertical** and **Flip Horizontal** modes are supported.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/text_left.png"> RtL Text

  In top-to-bottom, right-to-left script (TB-RL or vertical), writing starts from the top of the page and continues to the bottom, proceeding from right to left for new lines, pages numbered from right to left (from Wikipedia). The **RtL Text** mode can be applied to some traditional Chinese, Japanese and Korean books.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/single_column.png"> Single Column

  Some PDF books were scanned in a way that left and right pages were put in one image, resulting in a so called dual-spread page. In the scenario, **Single Column** mode plays a role. It splits a dual-spread page into two pages.

  **Single Column** mode can also be a conveniency for magazines and scientific papers that have two columns in a page.

  In **Single Column** mode, all pages except first and last page are splitted.

- Continuous scroll

  **Continuous scroll** has been perfectly implemented in all scenarios.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/lock.png"> Lock Stray

  When flinging or scrolling a zoomed page, it can hardly move in straight horizontal/vertical direction, and be annoying reading experience. Here the **Lock Stray** mode will make a help.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/crop_margin.png"> Crop Margin

  Crop page margins to get more efficient reading space. All document types are supported.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/focus.png"> Focus

  **Focus** mode will keep page position across zoomed pages. On moving to a definite page, eg. tapping to next/prev page, choosing on Toc/bookmark table, navigating through links or searching text, and skimming on page slider, it will present visible content area of new page in same position as the old one. Note that scroll/fling operation is an exception.

  On entering **Focus** mode, current page will zoom automatically to match screen in shorter dimension and center itself.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/smart_focus.png"> Smart Focus

  With scanned PDF books, content area scarcely appear exactly centered in a page. More probable it inclines toward left or right side. **Smart Focus** deals with the scenario. By adjusting the position of even or odd pages accordingly, it makes **Focus** mode behave smartly.

  **Smart Focus** must work with **Focus** mode to make sense.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/copy.png"> Copy Text

  Text copy can be accomplished by selecting text and tap **Copy Selection** button. In **Vertical Flip** mode, text selection can cross page boundary.

  During text selecting, page navigation operations still work.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/color.png"> Color Palette

  **Color Palette** are for maxmium legibility and are ideal for reducing eye strain conductive to focused reading.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/format.png"> Font Size

  Currenet font size can be changed in **Font Size** menu.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/contents.png"> Contents

  **Contents** menu includes following two functions:

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/toc.png"> Table of Contents

  **Table of Contents** will show up if the document has one. It supports multi-level headings, heading collapsing and expanding. It always keep sync with current page.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/bookmark.png"> Bookmarks

  **Bookmarks** works across reading sessions. To bookmark a page, just long press on blank area.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/link.png"> Activate Links

  **Activate Links** and make them navigable.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/search.png"> Search

  **Search** text and navigate through search goals.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/share.png"> Share Book

  Call Android **Share** menu.

- Scrollable Top Toobar

  Scrollable **Top Toolbar** can accommodate more buttons for extended funtions.

- Pros and Cons

  In case of big books(thousands of pages), PDFs were opened very quick, and EPUBs badly slow.

### Introduction in Youtube
[![Chaka Book Reader](https://img.youtube.com/vi/KkB2vlDj_6g/0.jpg)](https://www.youtube.com/watch?v=KkB2vlDj_6g)

### Usage
- A function button will show up in **Top Toolbar** when the corresponding function is applicable, otherwise it will be hidden.
- Long press a **Top Toolbar** button, to show it's tooltip.
- Tap in left/top side, to move a page backward (when in **not RtL Text** mode)
- Tap in right/bottom side, to move a page forward (when in **not RtL Text** mode)
- Tap in middle, to show/hide **Top Toolbar** and **Page Indicator**
- Press down one finger and move in any direction, to scrol a page
- Press down two fingers and move one, to zoom in/out pages
- Fling horizontally/vertically, to **Scroll Continuously**
- Under the combination of **Flip Horizontal and not Rtl Text** mode, or of **Flip Vertical and Rtl Text** mode, when pages are leaved between two pages, it will slide slowly into the near page that guarantees any page contents will not be cut off.
- Under the combination of **Flip Horizontal and Rtl Text** mode, or of **Flip Vertical and not Rtl Text** mode, pages can stay at any position which will never cut off page contents that makes reading across two pages comfortably.
- Long press one finger on text, to trigger a text selection session. Then press on left/right handles and move, to change selection area. Tap in selection area, to show/hide **Top Toolbar**. Then tap **Copy Selection** button, to copy the selected text to system clipboard and end the selection session. Tap out of selection area, to cancel the selection session.
- Long press one finger on blank area(not on text), to **Bookmark** current page.
- Reading status of **Flip Vertical**, **RtL Text** and **Single Column**, **Color Palette**, as well as **Font Size** and last read page number, will be remembered across reading sessions for per book.
- In general, to get the best reading experience mutiple function modes can be employed, adding appropriate screen orientation if needed.

### Credits
- [MuPDF Android Viewer](https://github.com/ArtifexSoftware/mupdf-android-viewer) and developers
- [MarkedView](https://github.com/mittsu333/MarkedView-for-Android) for help document rendering

### Contacts
- GitHub repo: [https://github.com/elementdavv/chaka](https://github.com/elementdavv/chaka)
- Email: elementdavv@hotmail.com
- Telegram: [@elementdavv](https://t.me/elementdavv)
- X(Twitter): [@elementdavv](https://x.com/elementdavv)

### Support
If you love Chaka, consider supporting or hiring the maintainer [@elementdavv](https://x.com/elementdavv) [![donate](https://raw.githubusercontent.com/elementdavv/chaka/master/resources/paypal-logo.png)](https://paypal.me/timelegend)
