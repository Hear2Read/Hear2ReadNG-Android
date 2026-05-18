This document outlines the build process for the Hear2ReadNG Android TTS app.

## External Requirements

This project requires Android NDK to build \(tested for NDK version 30\).

This project contains all the code required for the Hear2ReadNG Android app, but is missing several
binaries and other artifacts to be built successfully. These are:
- The relevant voice model ONNX files, their config files, as well as the prerecorded wavs in zip
  files. These are to be placed in their respective `<lang ISO2 code>/src/main/assets/`
  directories. However, this location, as well as the number of voices required will change for
  non production variants. See [Build Flavours](#build-flavours) below for further details.
- The h2r-ng engine binary. This is automatically built and included using ndk-build, but the source
  code needs to be present and pointed to in the root local.properties file. Ensure the following
  properties are present:
  - `sdk.dir=<Android SDK dir>`
  - `ndk.version=<version code of the installed NDK, tested with 30.0.14904198>`
  - `h2rng.dir=<location of the h2r-ng project folder>`
  
  Please refer to the h2r-ng project docs for additional requirements specific to that project. The
  main requirement for **_static_** builds is that the ONNX Runtime \(ORT\) needs to be built before
  running the Android build, and the env variable `ORT_LOCAL_PATH` . See
  [Build Flavours](#build-flavours) for further details regarding **_static_** and
  **_shared_** builds.
- The espeak-ng dictionary data, present within a espeak-ng-data dir at `/app/src/main/assets/`.
  This contains the relevant phonetic data required to convert text to phonemes.

## Build Flavours

This project uses Android's Play Asset Delivery \(PAD\) to deliver the voice files to user on
demand. However, testing with PAD delivery makes for a tedious iteration cycle. To deal with this,
the project build comes in several variants, combining multiple flavours and flavour dimensions.

Additionally, building ORT statically with Android NDK is only supported for arm64. If it needs to
be tested on an emulator, which typically requires x86 or x64, we will need to link ORT
dynamically. This has additional repercussions for the C++ STL used in the NDK build as well. 
\(_**Note**_: Although x64 is mentioned here, it is not supported or tested yet. Adding support will
require some modifications to the app build.gradle file, as well as to the CMake files for the
h2r-ng project. This unfortunately means that only x86 emulators are supported for now\).

The flavour dimensions, with the associated flavours, are:
- **delivery**: This points to whether the voices are to be delivered locally or through PAD. Given
  the differences in the API for these two, this is a nontrivial disparity requiring separate
  source sets. The flavours are:
  - **_pad_**: This is the production flavour, using PAD delivery, and expecting the voice files for
    all 14 languages. This ends up being quite massive \(~1GB\), which is why the JVM heap size has
    been increased to 4GB in the gradle.properties file.
  - **_padsmall_**: A more manageable variant of the **_pad_** build, with only a few language files
    required \(currently, English, Bengali and Kannada, but this can be changed easily by modifying
    the relevant portion in the app build.gradle file\). Otherwise identical to the **_pad_**
    flavour.
  - **_local_**: This flavour delivers the voices through the typical assets path. The voice files
    to be included for this flavour need to be placed in the `app/src/local/assets/Voices`
    Additionally, the prerecorded wav files of the letters needs to be placed in the relevant
    `app/src/local/assets/wavs/<voice ISO2 code>` directory, if required. The wav files need to be
    plain, and not zipped together. This variant has its own `applicationIdSuffix`, `".local"`,
    which means that it can be installed alongside one of the PAD builds on the same device.
- **stl**: This encompasses the two STL's that will be used, depending on whether the engine has
  ORT linked statically or dynamically. Android expects static builds by default, which allow for a
  single library \(.so file\) per build \(and per ABI\). Multiple binaries are only allowed in the
  shared STL, which will additionally require Android's `libc++_shared.so` to be shipped with the
  app. Thankfully, these modifications don't need to be done manually since we are using ndk-build.
  Since ORT doesn't support x86 and armv7 Android NDK builds, we provide support for these through
  dynamic linking alone \(the ORT .so file is pulled from the official Maven release in this case\).
  See the _h2r-ng_ project README for more details. The flavours associated with this dimension are:
  - **_static_**: The flavour used in production. Additionally, given the limitations of CMake in
    Android, this flavour only support the arm64 ABI, and conversely, arm64 is only supported in
    this flavour. We have consciously decided to not support 32 bit builds for production, given the
    low prevalence as well as the inherently lower compute capacity of such devices. This flavour
    requires an ORT static build, as described above
    \([External Requirements](#external-requirements)\).
  - **_shared_**: This flavour is used for testing on x86 emulators, as well as testing on armv7
    devices, if you want to for some reason. The advantage of this build is that it doesn't require
    ORT to be built, so it can be used as an initial test to make sure things are working fine.

### Allowed Variants

Given the 2 build types \(Debug & Release\), the 2 flavour dimensions, containing 3 and 2 flavours
respectively, the total number of variants comes up to an unwieldy `2*3*2 = 12`. Many of these are
extraneous as well, for instance debugging with a PAD build is not possible. Keeping this in mind,
only 5 variants are allowed and shown:
- **padStaticRelease**: The production build.
- **padsmallStaticRelease**: The testing version of the production build with fewer voices.
- **localStaticDebug**: Debug static build, can be run on a phone.
- **localSharedDebug**: Debug shared build, can be run on a x86 emulator.
- **localStaticRelease**: Testing build to be used when sharing the app for sideloading.



