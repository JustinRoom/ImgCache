package jsc.org.lib.img;

public interface IPageLoader {
    void onLoadPage(LazilyLoadableRecyclerView recyclerView, int loadedCount, int pageCapacity);
}
