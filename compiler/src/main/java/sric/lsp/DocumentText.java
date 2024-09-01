package sric.lsp;

import java.util.List;

/**
 *
 * @author yangjiandong
 */
public class DocumentText {
    private List<Integer> lineMap;
    public StringBuilder buffer;
    
    public int getLineStart(int lineNumber) {
        return this.lineMap.get(lineNumber);
    }
    
    public int getPosIndex(JsonRpc.Position pos) {
        return getLineStart(pos.line) + pos.character;
    }

    public void insert(JsonRpc.Range range, String text) {
        int fromIndex = getPosIndex(range.start);
        int toIndex = getPosIndex(range.end);
        
        this.buffer.replace(fromIndex, toIndex, text);   
        refreshLineMap();
    }
    
    public void setText(String text) {
        this.buffer = new StringBuilder(text);
        refreshLineMap();
    }
    
    private void refreshLineMap() {
        this.lineMap.clear();
        this.lineMap.add(0); // first line starts with the first character
         
        for(int i = 0; i < this.buffer.length(); i++) {
            char c = this.buffer.charAt(i);
            if(c == '\n') {
                this.lineMap.add(i + 1);
            }
        }       
    }
    
    public String getText() {
        return buffer.toString();
    }
}
