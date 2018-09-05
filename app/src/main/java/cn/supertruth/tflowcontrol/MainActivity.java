package cn.supertruth.tflowcontrol;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;

import cn.nexgo.tflow.TFlow;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testTFlow();
    }

    private TFlow tFlow;
    private void testTFlow(){
        tFlow = new TFlow();

        tFlow.setStatuesListenner(new TFlow.StatuesListenner(){

            @Override
            public void onFlowComplete() {
                System.out.println("flow onFlowComplete");
            }

            @Override
            public void onFlowCancel() {
                System.out.println("flow onFlowCancel");
            }

            @Override
            public void onActionStart(TFlow.IAction action) {
                System.out.println("start actino->"+action.toString());
            }

            @Override
            public void onActionFinish(TFlow.IAction action) {
                System.out.println("finish actino->"+action.toString());
            }
        });

        // action1 -> action2
        tFlow.addAction(action1, new TFlow.IActionLink<String>() {
            @Override
            public TFlow.IAction nextAction(String obj) {
                action2.setParams(obj);
                return action2;
            }
        }, Schedulers.io());
        // action2 -> action3
        tFlow.addAction(action2, new TFlow.IActionLink<Integer>() {
            @Override
            public TFlow.IAction nextAction(Integer obj) {
                action3.setParams(obj);
                return action3;
            }
        }, AndroidSchedulers.mainThread());
        // action3 -> action1
        tFlow.addAction(action3, new TFlow.IActionLink<Integer>() {
            @Override
            public TFlow.IAction nextAction(Integer obj) {
                if(obj == 6){
                    action1.setParams(obj);
                    return action1;
                }
                return null;
            }
        }, Schedulers.newThread());
        tFlow.startFlow(action1);       // 启动流程

//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                tFlow.cancelFlow(new TFlow.StopFlowListener() {
//                    @Override
//                    public void onStop() {
//                        System.out.println("onStop");
//                    }
//                });
//            }
//        }, 4000);
    }

    private TFlow.IAction action1 = new TFlow.IAction<Integer, String>(){
        @Override
        protected void onRun(final TFlow.IActionCB<String> cb) {
            new Thread(){
                @Override
                public void run() {
                    super.run();

                    System.out.println("action 1");
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    cb.finish("data from action 1");
                }
            }.start();
        }

        @Override
        public String toString() {
            return "action1";
        }
    };

    private TFlow.IAction action2 = new TFlow.IAction<String, Integer>(){
        @Override
        protected void onRun(final TFlow.IActionCB<Integer> cb) {
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    System.out.println("action 2");
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    cb.finish(3);
                }
            }.start();
        }

        @Override
        public String toString() {
            return "action2";
        }
    };

    private TFlow.IAction action3 = new TFlow.IAction<Integer, Integer>(){
        @Override
        protected void onRun(final TFlow.IActionCB<Integer> cb) {
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    System.out.println("action 3");
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    cb.finish(5);
                }
            }.start();
        }

        @Override
        public String toString() {
            return "action3";
        }
    };
}
