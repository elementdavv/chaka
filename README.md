## Chaka Book Reader
An Android book reader app based on MuPDF viewer. It does not care about book management, but dedicates to reading experience.

PDF, EPUB, MOBI, CBZ, FB2 and XPS documents are supported.

### Features
- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/flip_vertical.png"> Flip Vertical

  **Flip Vertical** and **Flip Horizontal** modes supported.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/text_left.png"> RtL Text

  In top-to-bottom, right-to-left script (TB-RL or vertical), writing starts from the top of the page and continues to the bottom, proceeding from right to left for new lines, pages numbered from right to left. The **RtL Text** mode can be applied to some traditional Chinese, Japanese and Korean books.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/single_column.png"> Single Column

  Some PDF books were scanned in a way that left and right pages were put in one image, resulting in a so called dual-spread page. In the scenario, **Single Column** mode plays a role. It splits a dual-spread page into two pages.

  **Single Column** mode only works with **WIDE** documents which page width is longer than page height. In **Single Column** mode, all pages except first and last page are splitted.

- Continuous scroll

  **Continuous scroll** has been perfectly implemented in all scenarios.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/lock.png"> Lock Stray

  When flinging or scrolling a zoomed page, it can hardly move in straight horizontal / vertical direction, and be annoying reading experience. Here the **Lock Stray** mode will make a help.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/crop_margin.png"> Crop Margin

  Crop page margins to get more efficient reading space. All document types are supported.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/focus.png"> Focus

  **Focus** mode can retent page position across zoomed pages. When page flips. it presents visible content area of new page in same position as the old one.

  On entering **Focus** mode, current page will zoom automatically to match screen in shorter dimension and center itself.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/smart_focus.png"> Smart Focus

  With scanned PDF books, content area scarcely appear exactly centered in a page. More probable it inclines toward left or right side. **Smart Focus** deals with the scenario. By adjusting the position of even or odd pages accordingly, it makes **Focus** mode behave smartly.

  **Smart Focus** must work with **Focus** mode to make sense.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/copy.png"> Copy Text

  **Text Copy** can be achived by selecting text and tap **Copy** button. In **Vertical Flip** mode, text selection can cross multi-pages. The selection state does not interfere navigation functions.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/format.png"> Font size

  Current font size is indicated in **Font Size** menu.

- <img src="https://raw.githubusercontent.com/elementdavv/chaka/master/resources/toc.png"> Table of Contents

  **Table of Contents** supports multi-level headings, and headings collapsing / expanding. It keeps sync with the current page number on opening.

- Scrollable Toobar

  The Scrollable Top Toolbar can accommodate many buttons. The buttons will show up when the corresponding functions are appliable.

- Pros and Cons

  For Big books (thousands of pages), PDFs were opened quickly, EPUBs were opened slowly.

### Introduction in Youtube
[![Chaka Book Reader](https://img.youtube.com/vi/KkB2vlDj_6g/0.jpg)](https://www.youtube.com/watch?v=KkB2vlDj_6g)

### Usage
- Tap in left/top side, to move a page backward
- Tap in right/bottom side, to move a page forward
- Tap in middle, to show/hide Top Toolbar and Page Indicator
- Press down and move horizontally/vertically, to move page position
- Fling horizontally/vertically, to scroll continuously
- With combination of **Flip Horizontal and Rtl Text** mode, or of **Flip Vertical and not Rtl Text** mode, pages can stay at any position.  
- Press down two fingers and move one of them, to zoom in/out page
- Long press on text, to trigger text selection. Then press down on left/right handles and move, to change selection area. Tap in selection area, to show/hide Top Toolbar. Tap **Copy** button, to copy text and end text selection. Tap out of selection area, to cancel text selection.
- Long press on Top Toolbar buttons, to show buttons tooltip.
- In general, you can get best reading experience by using combination of button modes, and screen orientation.

### Credits
- [MuPDF Viewer](https://github.com/ArtifexSoftware/mupdf-android-viewer)
- [MarkedView](https://github.com/mittsu333/MarkedView-for-Android)

### Contacts
- GitHub repo: [https://github.com/elementdavv/chaka](https://github.com/elementdavv/chaka)
- Email: elementdavv@hotmail.com
- Telegram: [@elementdavv](https://t.me/elementdavv)
- X(Twitter): [@elementdavv](https://x.com/elementdavv)

### Donation
If you want to support my work you could donate by [![donate](https://raw.githubusercontent.com/elementdavv/chaka/master/resources/paypal-logo.png)](https://paypal.me/timelegend)
