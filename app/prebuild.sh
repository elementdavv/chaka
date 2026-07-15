#! /bin/bash

set -x

rm -rf ../jni/libmupdf/thirdparty/curl/tests/data/
rm -rf ../jni/libmupdf/thirdparty/leptonica/prog/fuzzing/
rm -rf ../jni/libmupdf/thirdparty/lcms2/testbed/
rm -rf ../jni/libmupdf/thirdparty/lcms2/Projects/VC2013/lcms2mt.sln
rm -rf ../jni/libmupdf/thirdparty/lcms2/utils/delphi/
rm -rf ../jni/libmupdf/thirdparty/harfbuzz/test/fuzzing
rm -rf ../jni/libmupdf/thirdparty/harfbuzz/test/api/fonts/clusterfuzz-testcase-minimized-hb-shape-fuzzer-5753845452636160
rm -rf ../jni/libmupdf/thirdparty/extract/test
rm -rf ../libarchive/src/main/jni/external/libarchive/contrib/oss-fuzz/corpus.zip
rm -rf ../libarchive/src/main/jni/external/mbedtls/framework/data_files/
rm -rf ../libarchive/src/main/jni/external/mbedtls/programs/fuzz/corpuses/
rm -rf ../libarchive/src/main/jni/external/lz4/tests/
rm -rf ../libarchive/src/main/jni/external/zstd/tests/
find ../ -name build.gradle -exec sed -i -E '/^\s{12}maven\s*\{\s*$/,/^\s{12}\}\s*$/d' {} \;
find ../ -name build.gradle -exec sed -i -E '/maven\s*\{/d' {} \;
