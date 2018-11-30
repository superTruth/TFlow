package cn.nexgo.tflow;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;

/***************************************************************************************************
 *                                  Copyright (C), Nexgo Inc.                                      *
 *                                    http://www.nexgo.cn                                          *
 ***************************************************************************************************
 * usage           : 
 * Version         : 1
 * Author          : Truth
 * Date            : 2018/4/18
 * Modify          : create file
 **************************************************************************************************/
public class TFlow {
    private List<InternalAction> internalActions = new ArrayList<>();

    public <I, O> void addAction(IAction<I, O> action, IActionLink<O> subcrib){
        addAction(action, subcrib, null);
    }

    public <I, O> void addAction(IAction<I, O> action, IActionLink<O> subcrib, Scheduler scheduler){

        InternalAction internalAction = new InternalAction();
        internalAction.action = action;
        internalAction.actionLink = subcrib;
        internalAction.action.setTag(internalActions.size());

        internalAction.scheduler = scheduler;

        internalActions.add(internalAction);
    }

    private boolean running = false;
    private InternalAction runningAction;
    public synchronized void startFlow(IAction action){
        if(running){
            return;
        }

        stopFlowFlag = false;

        for (InternalAction internalAction : internalActions) {
            if(internalAction.action == action){
                runningAction = internalAction;
            }
        }

        if(runningAction == null){
            return;
        }

        running = true;

        if(statuesListenner != null){
            statuesListenner.onFlowStart();
        }

        flowLoop();
    }

    // 循环执行
    private void flowLoop(){

        if(stopFlowFlag){
            running = false;

            if(stopFlowListener != null){
                stopFlowListener.onStop();
            }
            if(statuesListenner != null){
                statuesListenner.onFlowCancel();
            }

            return;
        }

        if((runningAction == null) || (runningAction.action.getTag() == -1)){  // 未添加映射的action
            running = false;
            if(statuesListenner != null){
                statuesListenner.onFlowComplete();
            }
            return;
        }

        if(runningAction.actionCB == null){
            runningAction.actionCB = new IActionCB() {
                @Override
                public synchronized void finish(Object obj) {
                    if(!this.getParent().canCB){
                        return;
                    }
                    runningAction.canCB = false;

                    if(statuesListenner != null){  //  action结束状态回调
                        statuesListenner.onActionFinish(runningAction.action);
                    }

                    IAction action = runningAction.actionLink.nextAction(obj);  // 寻找下一个action
                    if(action == null){
                        runningAction = null;
                        flowLoop();
                        return;
                    }

                    runningAction = internalActions.get(action.getTag());

                    flowLoop();  // 继续执行action循环
                }
            };
            runningAction.actionCB.setParent(runningAction);
        }

        Observable<Object> objectObservable = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                runningAction.canCB = true;
                if(statuesListenner != null){
                    statuesListenner.onActionStart(runningAction.action);
                }
                runningAction.action.onRun(runningAction.actionCB);
            }
        });

        if(runningAction.scheduler != null){
            objectObservable = objectObservable.subscribeOn(runningAction.scheduler);
        }
        objectObservable.subscribe();
    }

    /**
     * action
     * @param <I> input params
     * @param <O> output params
     */
    public abstract static class IAction<I, O>{
        private int tag = -1;
        protected abstract void onRun(IActionCB<O> cb);

        private I params;
        public void setParams(I params){
            this.params = params;
        }
        public I getParams(){
            return params;
        }

        final int getTag() {
            return tag;
        }

        final void setTag(int tag) {
            this.tag = tag;
        }
    }

    /**
     * action link
     * @param <O> action output params type
     */
    public interface IActionLink<O> {
        IAction nextAction(O obj);
    }

    /**
     * action result
     * @param <O> action output params type
     */
    public abstract static class IActionCB<O>{
        public abstract void finish(O obj);

        private InternalAction parent;
        InternalAction getParent(){
            return this.parent;
        }
        void setParent(InternalAction parent){
            this.parent = parent;
        }
    }

    private static class InternalAction{
        public IAction action;
        public IActionLink actionLink;
        public Scheduler scheduler;

        public boolean canCB = false;

        public IActionCB actionCB;
    }


    /**
     * cancel flow
     */
    private boolean stopFlowFlag = false;
    private StopFlowListener stopFlowListener;
    public void cancelFlow(StopFlowListener listener){
        stopFlowListener = listener;
        stopFlowFlag = true;
    }

    public interface StopFlowListener{
        void onStop();
    }

    /**
     * StatuesListenner
     */
    private StatuesListenner statuesListenner;
    public void setStatuesListenner(StatuesListenner statuesListenner){
        this.statuesListenner = statuesListenner;
    }

    public interface StatuesListenner{
        void onFlowStart();
        void onFlowComplete();
        void onFlowCancel();
        void onActionStart(IAction action);
        void onActionFinish(IAction action);
    }

    /**
     * 判断流程是否真正进行
     * @return
     */
    public boolean isRunning(){
        return running;
    }

}
