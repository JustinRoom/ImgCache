package jsc.org.lib.img.selector.model;

import java.util.ArrayList;
import java.util.List;

public class LocalMediaFolder {
    public String name;
    public String path;
    public List<LocalMedia> images = new ArrayList<>();
    public boolean selected = false;

    public int count() {
        return images.size();
    }
}
