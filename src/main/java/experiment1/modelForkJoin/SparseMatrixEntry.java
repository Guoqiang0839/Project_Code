package experiment1.modelForkJoin;

class SparseMatrixEntry {
    public int row;
    public int col;
    public double value;

    public SparseMatrixEntry(int row, int col, double value) {
        this.row = row;
        this.col = col;
        this.value = value;
    }
}