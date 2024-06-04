package configgen.data;

import java.util.List;

public class FakeRows {
    static class FakeRow implements CfgData.DRawRow {
        private final String[] cells;

        FakeRow(String[] cells) {
            this.cells = cells;
        }

        @Override
        public String cell(int c) {
            return cells[c];
        }

        @Override
        public boolean isCellNumber(int c) {
            return false;
        }

        @Override
        public int count() {
            return cells.length;
        }
    }


    static List<FakeRow> getFakeRows() {
        return List.of(
                new FakeRows.FakeRow(new String[]{"id", "记录"}),
                new FakeRows.FakeRow(new String[]{"id", "note"}),
                new FakeRows.FakeRow(new String[]{"1", "note1"}),
                new FakeRows.FakeRow(new String[]{"#分割行"}),
                new FakeRows.FakeRow(new String[]{"2", "note2"})
        );
    }

    static List<FakeRows.FakeRow> getFakeRows2() {
        return List.of(
                new FakeRows.FakeRow(new String[]{"id", "记录"}),
                new FakeRows.FakeRow(new String[]{"id", "note"}),
                new FakeRows.FakeRow(new String[]{"3", "note3"}),
                new FakeRows.FakeRow(new String[]{"4", "note4"})
        );
    }


    static List<FakeRows.FakeRow> getFakeColumnRows() {
        return List.of(
                new FakeRows.FakeRow(new String[]{"id", "id", "1", "#分割列，忽略", "2"}),
                new FakeRows.FakeRow(new String[]{"策划注释", " "}), // whitespace as empty
                new FakeRows.FakeRow(new String[]{"说明", "note", "note1", "", "note2"})
        );
    }

}
