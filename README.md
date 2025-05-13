## Chaka Book Reader
An Android reader app committed to improving reading experience.

PDF, EPUB, MOBI, CBZ, FB2 and XPS documents are supported.

### Features
- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/flip_vertical.png"> Flip Vertical

  **Flip Vertical** and **Flip Horizontal** modes supported.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/text_left.png"> RtL Text

  In top-to-bottom, right-to-left script (TB-RL or vertical), writing starts from the top of the page and continues to the bottom, proceeding from right to left for new lines, pages numbered from right to left (from Wikipedia). The **RtL Text** mode can be applied to some traditional Chinese, Japanese and Korean books.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/single_column.png"> Single Column

  Some PDF books were scanned in a way that left and right pages were put in one image, resulting in a so called dual-spread page. In the scenario, **Single Column** mode plays a role. It splits a dual-spread page into two pages.

  The **WIDE** document restriction was removed now. **Single Column** mode can be a conveniency for magazines and scientific papers with two columns in a page.

  In **Single Column** mode, all pages except first and last page are splitted.

- Continuous scroll

  **Continuous scroll** has been perfectly implemented in all scenarios.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/lock.png"> Lock Stray

  When flinging or scrolling a zoomed page, it can hardly move in straight horizontal / vertical direction, and be annoying reading experience. Here the **Lock Stray** mode will make a help.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/crop_margin.png"> Crop Margin

  Crop page margins to get more efficient reading space. All document types are supported.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/focus.png"> Focus

  **Focus** mode can retent page position across zoomed pages. On moving to a definite page, ie. tapping to next/prev page, choosing on Toc/bookmark table, navigating on links, searching text, and skimming on page slider etc., it presents visible content area of new page in same position as the old one. Note that scroll operation is an exception.

  On entering **Focus** mode, current page will zoom automatically to match screen in shorter dimension and center itself.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/smart_focus.png"> Smart Focus

  With scanned PDF books, content area scarcely appear exactly centered in a page. More probable it inclines toward left or right side. **Smart Focus** deals with the scenario. By adjusting the position of even or odd pages accordingly, it makes **Focus** mode behave smartly.

  **Smart Focus** must work with **Focus** mode to make sense.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/copy.png"> Copy Text

  **Text Copy** can be accomplished by selecting text and tap **Copy** button. In **Vertical Flip** mode, text selection can cross page boundry.

  While text selecting, page navigation still work.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/format.png"> Font size

  Current font size was indicated in **Font Size** menu.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/contents.png"> Contents

  **Contents** menu includes following two functions:

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/toc.png"> Table of Contents

  **Table of Contents** will show up if the document has one. It supports multi-level headings, heading collapsing and expanding. It always keep sync with current page.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/bookmark.png"> Bookmarks

  **Bookmarks** works across reading sessions. To bookmark a page, just long press on blank area.

- Scrollable Toobar

  The Scrollable Top Toolbar can accommodate more buttons. A button will show up when the corresponding function is applicable, otherwise it will be hidden.

- Pros and Cons

  In case of Big books (thousands of pages), PDFs were opened very quick, and EPUBs badly slow.

### Introduction in Youtube
[![Chaka Book Reader](https://img.youtube.com/vi/KkB2vlDj_6g/0.jpg)](https://www.youtube.com/watch?v=KkB2vlDj_6g)

### Usage
- Long press on Top Toolbar buttons, to show button tooltip.
- Tap in left/top side, to move a page backward
- Tap in right/bottom side, to move a page forward
- Tap in middle, to show/hide Top Toolbar and Page Indicator
- Press down and move in any direction, to move page position
- Fling horizontally/vertically, to scroll continuously
- With the combination of **Flip Horizontal and Rtl Text** mode, or of **Flip Vertical and not Rtl Text** mode, pages can stay at any position.
- Press down two fingers and move one, to zoom in/out page
- Long press on text, to trigger a text selection session. Then press down on one of left/right handles and move, to change selection area. Tap in selection area, to show/hide Top Toolbar. Tap **Copy** button, to copy text and end the selection session. Tap out of selection area, to cancel the selection session.
- Long press on blank area (not on text), to bookmark current page.
- In general, one can get the best reading experience by combining mutiple function modes, adding appropriate screen orientation if needed.

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
