package com.turnik.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private final TreeMap<String,ArrayList<Integer>> log = new TreeMap<>();
    private LinearLayout root, history;
    private TextView todayText, totalText, streakText, recordText;
    private Graph graph;
    private final int dark=Color.rgb(18,25,34), accent=Color.rgb(25,130,103);
    private final SimpleDateFormat dateFmt=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
    String today(){return dateFmt.format(new Date());}
    int sum(String day){int n=0; ArrayList<Integer> l=log.get(day); if(l!=null)for(int v:l)n+=v;return n;}
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(dark);getWindow().setNavigationBarColor(dark);load();build();new Updates(this).check(false);}
    TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(dark);if(bold)t.setTypeface(null,1);return t;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    void add(LinearLayout p,View v,int top){LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,-2);q.topMargin=dp(top);p.addView(v,q);}
    Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(17);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));return b;}
    void build(){ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root=column();root.setPadding(dp(20),dp(24),dp(20),dp(36));root.setBackgroundColor(Color.rgb(247,249,248));scroll.addView(root);setContentView(scroll);
        add(root,text("ТУРНИК  💪",27,true),0);add(root,text("Каждый день становись сильнее",14,false),3);
        todayText=text("",50,true);add(root,todayText,24);add(root,text("подтягиваний сегодня",16,false),0);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);for(int n:new int[]{1,5,10}){Button b=button("+"+n);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(56),1);p.setMargins(dp(2),0,dp(2),0);actions.addView(b,p);b.setOnClickListener(v->record(n));}add(root,actions,20);
        Button custom=button("Записать подход");add(root,custom,8);custom.setOnClickListener(v->askCount());Button update=button("Проверить обновления");add(root,update,8);update.setOnClickListener(v->new Updates(this).check(true));
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(0);totalText=text("",15,true);streakText=text("",15,true);recordText=text("",15,true);for(TextView t:new TextView[]{totalText,streakText,recordText}){t.setGravity(Gravity.CENTER);stats.addView(t,new LinearLayout.LayoutParams(0,dp(66),1));}add(root,stats,16);
        add(root,text("Прогресс за 14 дней",20,true),18);graph=new Graph(this);add(root,graph,8);
        add(root,text("История подходов",20,true),20);history=column();add(root,history,8);
        Button export=button("Поделиться резервной копией");add(root,export,22);export.setOnClickListener(v->exportData());refresh();}
    void askCount(){EditText e=new EditText(this);e.setInputType(2);e.setHint("Например, 8");new AlertDialog.Builder(this).setTitle("Сколько подтягиваний?").setView(e).setNegativeButton("Отмена",null).setPositiveButton("Добавить",(d,w)->{try{int n=Integer.parseInt(e.getText().toString());if(n>0&&n<=1000)record(n);else Toast.makeText(this,"Введите число от 1 до 1000",Toast.LENGTH_SHORT).show();}catch(Exception ex){Toast.makeText(this,"Введите число",Toast.LENGTH_SHORT).show();}}).show();}
    void record(int n){if(!log.containsKey(today()))log.put(today(),new ArrayList<>());log.get(today()).add(n);save();refresh();}
    void remove(String day,int idx){ArrayList<Integer> a=log.get(day);if(a==null||idx>=a.size())return;a.remove(idx);if(a.isEmpty())log.remove(day);save();refresh();}
    void refresh(){todayText.setText(""+sum(today()));int total=0,best=0;for(String d:log.keySet()){int n=sum(d);total+=n;best=Math.max(best,n);}int streak=0;Calendar c=Calendar.getInstance();if(sum(today())==0)c.add(Calendar.DAY_OF_MONTH,-1);while(sum(dateFmt.format(c.getTime()))>0){streak++;c.add(Calendar.DAY_OF_MONTH,-1);}totalText.setText("ВСЕГО\n"+total);streakText.setText("СЕРИЯ\n"+streak+" дн.");recordText.setText("РЕКОРД\n"+best);graph.invalidate();history.removeAllViews();int shown=0;for(String day:log.descendingKeySet()){if(shown++>=30)break;TextView date=text(day+"  •  "+sum(day)+" раз",16,true);add(history,date,12);ArrayList<Integer> a=log.get(day);for(int i=a.size()-1;i>=0;i--){final int idx=i;TextView item=text("Подход: "+a.get(i)+"    ✕ удалить",15,false);item.setPadding(dp(8),dp(9),dp(8),dp(9));add(history,item,0);item.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Удалить этот подход?").setNegativeButton("Нет",null).setPositiveButton("Удалить",(d,w)->remove(day,idx)).show());}}if(log.isEmpty())add(history,text("Пока пусто. Начни с первого подхода!",15,false),8);}
    void load(){try{JSONObject j=new JSONObject(getPreferences(0).getString("log","{}"));Iterator<String> it=j.keys();while(it.hasNext()){String k=it.next();JSONArray a=j.getJSONArray(k);ArrayList<Integer> l=new ArrayList<>();for(int i=0;i<a.length();i++)l.add(a.getInt(i));log.put(k,l);}}catch(Exception ignored){}}
    String json(){JSONObject j=new JSONObject();try{for(String k:log.keySet()){JSONArray a=new JSONArray();for(int n:log.get(k))a.put(n);j.put(k,a);}}catch(Exception ignored){}return j.toString();}
    void save(){getPreferences(0).edit().putString("log",json()).apply();}
    void exportData(){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"Турник: резервная копия");i.putExtra(Intent.EXTRA_TEXT,json());startActivity(Intent.createChooser(i,"Сохранить данные"));}
    class Graph extends View{Paint p=new Paint(3);Graph(Context c){super(c);setMinimumHeight(dp(200));}protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),bottom=h-dp(30),top=dp(18);int[] vals=new int[14];Calendar cal=Calendar.getInstance();int max=1;for(int i=13;i>=0;i--){vals[i]=sum(dateFmt.format(cal.getTime()));max=Math.max(max,vals[i]);cal.add(Calendar.DAY_OF_MONTH,-1);}p.setColor(Color.rgb(226,231,230));p.setStrokeWidth(dp(1));for(int k=0;k<=4;k++){float y=top+(bottom-top)*k/4;c.drawLine(0,y,w,y,p);}float slot=w/14f;for(int i=0;i<14;i++){float x=i*slot+slot*.18f;float bar=vals[i]*(bottom-top)/max;p.setColor(accent);c.drawRoundRect(x,bottom-bar,x+slot*.64f,bottom,dp(4),dp(4),p);if(i%2==0){p.setColor(dark);p.setTextSize(dp(10));c.drawText(""+(i-13),x,bottom+dp(17),p);}}p.setColor(dark);p.setTextSize(dp(11));c.drawText("Макс: "+max,dp(2),dp(12),p);}}
}
