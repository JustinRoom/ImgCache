/*
 * Copyright (c) 2023.
 *
 * Author: JiangShiCheng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jsc.org.lib.img;

public class ImgUtils {

    public static int[] formatSize(String baseOn, int baseWidth, int baseHeight, int value) {
        int base = "width".equals(baseOn) ? baseWidth : baseHeight;
        int multi = value / base;
        if (multi * base < value) {
            multi++;
        }
        multi = Math.max(1, multi);
        return new int[]{baseWidth * multi, baseHeight * multi};
    }

    public static void asBitmap(String netUrl, ImgCallback callback) {
        ImgCacheManager.defaultImgCache().asBitmap(netUrl, callback);
    }
}
