## v1.27.1a.23
- include Common Vulnerabilities and Exposures(CVE) 1.25-1.27
- improve html table render
- decrease page margin for reflowable documents to get more render space
- same document opened in same window in recent apps overview when opened through same route
- improve transparent system bars display both in new Android version and older versions
- improve page display effect

## v1.24.9a.22
- text select toolbar implemented. operations of copy, share, translate and more are supported.

## v1.24.9a.21
- support android 15(sdk 35)
- support 16KB page size
- support F-Droid arch specific builds
- support code optimizer
- same document opened in same window in recent apps view
- improve display experience

## v1.24.9a.20
- add fullscreen option
- add toolbar on bottom option
- add global default options
- add left/right swipe gestures support in contents/help window
- use double tap to make bookmarks instead of long press
- fix a bug with extreme long txt file

## v1.24.9a.19
- open every document in new window
- support full screen with notch
- remember all reading status, including page scale, position and all enable button state
- improve and fix font size settings
- improve color palette settings
- improve toolbar alignment on switching screen orientation
- loading progress does not interrupted by random touch
- search progress does not interrupted by random touch

## v1.24.9a.18
- add color palette
- add book sharing
- add loading progress for epubs
- remember reading status
- compact fontsize options
- correct gzip mimetype

## v1.24.9a.17
- fix a bug which cause cbz read only first page
- zip package with all files of images read as cbz
- zoomed page gets clear when not in focus
- test interval of help page from per week to per day

## v1.24.9a.16
- add support of txt/html/office(docx/xlsx/pptx) documents and zip/gzip packages
- MuPDF was improved

## v1.24.9a.14
- add bookmark function
- sync with MuPDF Viewer 1.25.6a (not MuPDF)

## v1.24.9a.12
- add online help
- fix smart focus mode not working on vertical flipping
- fix text blurred on zoomed splitted right page
- fix text not shown in search dialog
- fix last page not remembered on exit with flowable documents (like epub)
- fix possible bad toolbar alignment on switching screen orientation
- remove storage permission
- remove requirement of WIDE documents for single column mode
- prevent restart on configuration change
- optimize input mode (test)
- improve interface experience

## v1.24.9a.11
- Add text copy function

## v1.24.9a.10
- add Lock Stray function
- improve flowable documents display speed
- formalize flowable document display style
- make top toolbar buttons show up when available
- change app icon and fdroid metadata

## v1.24.9a.9
- f-droid release
- improve crop margin render for an image as a page
- update overscroll edge effect for top toolbar and ToC

## v1.24.9a.8
- add crop margin function
- add indent to ToC multi levels
- fix single column display bug introduced in last release
- fix page overlay bug
- search text now trimmed before search
- stability and other experiences improvement

## v1.24.9a.7
- fix text blurred bug
- fix focus mode bug

## v1.24.9a.6
- add continuous scroll
- make book title display adaptive to screen width
- improve scroll and scale experience
- improve Right to Left Forward mode

## v1.24.9a.5
- add smart focus mode
- add title and buttons tooltip
- make toolbar scrollable
- apply material theme
- improve search experience
- bug fixes

## v1.24.9a.4
- add page position preserving function to focus mode.
- fix bitmap oom crash bug in single column mode.
- fix splitted page scaled crash bug.
- fix splitted page wrong order bug in rtl text mode.

## v1.24.9a.3
- add support of fill screen mode which enlarge the page to best position.
- fix a bug in outline function with multiple levels of headings.

## v1.24.9a.2
- add current selection indication in layout menu with flowable documents.
- rewrite outline function to support collapsing and expanding of headings.

## v1.24.9a.1
- first release, forked from mupdf-android-viewer
- add support of vertical page flipping.
- add support of right to left text direction.
- add support of single column mode. It splits a double-page spread image from a scanned pdf book into two seperated pages.
- improve navigation and visual experience.
