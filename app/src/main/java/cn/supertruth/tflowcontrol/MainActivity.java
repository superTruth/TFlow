package cn.supertruth.tflowcontrol;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import cn.nexgo.tflow.TFlow;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testTFlow();
    }

    private void testTFlow(){
        TFlow tFlow = new TFlow();

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
                if(obj == null){
                    return action1;
                }
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

        tFlow.startFlow(action1);
    }

    private TFlow.IAction action1 = new TFlow.IAction<Integer, String>(){
        @Override
        protected void onRun(final TFlow.IActionCB<String> cb) {
            new Thread(){
                @Override
                public void run() {
                    super.run();

                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    cb.finish("data from action 1");
                }
            }.start();
        }
    };

    private TFlow.IAction action2 = new TFlow.IAction<String, Integer>(){
        @Override
        protected void onRun(final TFlow.IActionCB<Integer> cb) {
            new Thread(){
                @Override
                public void run() {
                    super.run();

                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    cb.finish(3);
                }
            }.start();
        }
    };

    private TFlow.IAction action3 = new TFlow.IAction<Integer, Integer>(){
        @Override
        protected void onRun(final TFlow.IActionCB<Integer> cb) {
            new Thread(){
                @Override
                public void run() {
                    super.run();

                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    cb.finish(6);
                }
            }.start();


        }
    };
}
