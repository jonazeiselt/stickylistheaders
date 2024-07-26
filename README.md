# StickyListHeaders

Modification of StickyListHeaders (https://github.com/emilsjolander/StickyListHeaders).
Supports newer app versions and only supports a list view with sticky headers.

## Installation

In your build.gradle file (project level):

```diff
 repositories {
+   maven {
+       url 'https://jitpack.io'
+   }
 }
```

In your build.gradle file (app level):

```diff
 dependencies {
+   implementation 'com.github.jonazeiselt:stickylistheaders:<version>'
 }
```
