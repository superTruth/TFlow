package cn.supertruth.tflowcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import cn.truth.tflow.LoopAction;
import cn.truth.tflow.TFlow;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends Activity {

    private Handler handler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testTFlow();

//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                tFlow.cancelFlow(new TFlow.StopFlowListener() {
//                    @Override
//                    public void onStop() {
//                        System.out.println("tflow onStop");
//                    }
//                });
//            }
//        }, 3000);
    }

    private TFlow tFlow;
    private void testTFlow(){
        tFlow = new TFlow();

        tFlow.setStatuesListener(new TFlow.StatuesListener(){

            @Override
            public void onFlowStart() {
                System.out.println("flow onFlowStart");
            }

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
                action2.setRunParameters(action2.getRunParameters().setRunDelay(3000));
                return action2;
            }

        }, new TFlow.RunParameters().setScheduler(Schedulers.io()).setTimeout(1000));
        // action2 -> action3
        tFlow.addAction(action2, new TFlow.IActionLink<Integer>() {
            @Override
            public TFlow.IAction nextAction(Integer obj) {
                action3.setParams(obj);
                return action3;
            }
        }, new TFlow.RunParameters().setScheduler(Schedulers.io()));
        // action3 -> action1
        tFlow.addAction(action3, new TFlow.IActionLink<Integer>() {
            @Override
            public TFlow.IAction nextAction(Integer obj) {
                if(obj == 6){
                    action1.setParams(obj);
                    return action1;
                }

                List<String> params = new ArrayList<>();
                params.add("param1");
                params.add("param2");
                params.add("param3");

                loopAction4.setParams(params);
                return loopAction4;
            }
        },  new TFlow.RunParameters().setScheduler(Schedulers.io()));

        // action 4
        tFlow.addAction(loopAction4, new TFlow.IActionLink<String>() {
            @Override
            public TFlow.IAction nextAction(String obj) {
                System.out.println("action4 ret ->"+obj);
                return null;
            }
        });

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

                    cb.finish("data from action 1");
                }
            }.start();
        }

        @Override
        protected void onTimeout() {

        }

        @Override
        public void cancel() {
            System.out.println("action1 cancel");
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
                    cb.finish(3);
                }
            }.start();
        }

        @Override
        protected void onTimeout() {

        }

        @Override
        public String toString() {
            return "action2";
        }

        @Override
        public void cancel() {
            System.out.println("action2 cancel");
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
        protected void onTimeout() {

        }

        @Override
        public String toString() {
            return "action3";
        }

        @Override
        public void cancel() {
            System.out.println("action3 cancel");
        }
    };


    private LoopAction<List<String>, String, String, String> loopAction4 = new LoopAction<List<String>, String, String, String>(new TFlow.IAction<String, String>() {
        @Override
        protected void onRun(TFlow.IActionCB<String> cb) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cb.finish(getParams());
        }

        @Override
        protected void onTimeout() {

        }

        @Override
        public void cancel() {

        }
    }) {

        @Override
        protected void onTimeout() {

        }

        @Override
        public void cancel() {

        }

        @Override
        public String toString() {
            return "loopAction4";
        }

        private int index = 0;
        @Override
        protected void loopStart() {
            index = 0;
        }

        @Override
        protected String getFinalRet() {
            return sb.toString();
        }

        private StringBuilder sb = new StringBuilder();
        @Override
        protected void setOneRet(String out) {
            sb.append(out);
            System.out.println("loopAction4 setOneRet->"+out+"\n");
        }

        @Override
        protected String pickOneEvent() {
            System.out.println("loopAction4 pickOneEvent");

            List<String> events = getParams();
            if(events == null){
                return null;
            }

            if(index >= events.size()){  // 结束
                return null;
            }

            String event = events.get(index);
            index++;

            return event;
        }
    };
}
