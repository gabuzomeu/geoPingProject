package eu.ttbox.geoping.ui.lock;


public interface SecurityMessageDisplay {

    public void setMessage(CharSequence msg, boolean important);

    public void setMessage(int resId, boolean important);

    public void setMessage(int resId, boolean important, Object... formatArgs);

    public void setTimeout(int timeout_ms);

    public void showBouncer(int animationDuration);

    public void hideBouncer(int animationDuration);

}
