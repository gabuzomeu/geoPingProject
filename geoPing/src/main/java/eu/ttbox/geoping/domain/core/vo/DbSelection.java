package eu.ttbox.geoping.domain.core.vo;


public class DbSelection {

    public final String selection ;
    public final String[] selectionArgs ;

    public DbSelection( String selection ,String[] selectionArgs  ) {
        this.selection = selection;
        this.selectionArgs = selectionArgs;
    }

}
