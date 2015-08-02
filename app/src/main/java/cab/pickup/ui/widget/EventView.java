package cab.pickup.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import cab.pickup.R;
import cab.pickup.ui.activity.MyActivity;

public class EventView extends LinearLayout{
    public String title="Not Init";
    public String time="infinite time ago";
    public int icon= R.drawable.user;

    TextView mTitle;
    TextView mTime;
    ImageView mIcon;

    MyActivity context;

    public EventView(Context context) {
        super(context);

        this.context = (MyActivity) context;
        init();
    }

    private void init() {
        LayoutInflater inflater= context.getLayoutInflater();
        LinearLayout eventView = (LinearLayout) inflater.inflate(R.layout.event_view, this, true);

        mTitle = ((TextView)eventView.findViewById(R.id.event_title));
        mIcon = ((ImageView)eventView.findViewById(R.id.event_icon));
        mTime = ((TextView)eventView.findViewById(R.id.event_time));

        updateView();
    }

    public void updateView(){
        mTitle.setText(title);
        mTime.setText(time);
        mIcon.setImageDrawable(context.getResources().getDrawable(icon));
    }

    public void setContent(String title, String time, int icon){
        this.time=time;
        this.title=title;
        this.icon=icon;

        updateView();
    }
}