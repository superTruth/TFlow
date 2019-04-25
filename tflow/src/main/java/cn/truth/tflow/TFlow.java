package cn.truth.tflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/***************************************************************************************************
 *                                  Copyright (C), Truth Inc.                                      *
 *                                    http://www.truth.cn                                          *
 ***************************************************************************************************
 * usage           : 
 * Version         : 1
 * Author          : Truth
 * Date            : 2018/4/18
 * Modify          : create file
 **************************************************************************************************/
public class TFlow {
    private List<InternalAction> internalActions = new ArrayList<>();

    public <I, O> void addAction(IAction<I, O> action, IActionLink<O> subscribe, RunParameters runParameters) {
        action.runParameters = runParameters;
        addAction(action, subscribe);
    }

    public <I, O> void addAction(IAction<I, O> action, IActionLink<O> subscribe) {

        InternalAction internalAction = new InternalAction();
        internalAction.action = action;
        internalAction.actionLink = subscribe;
        internalAction.action.setTag(internalActions.size());

        if (internalAction.action.runParameters == null) {
            internalAction.action.runParameters = new RunParameters();
        }

        internalActions.add(internalAction);
    }

    private boolean running = false;
    private InternalAction runningAction;

    public synchronized void startFlow(IAction action) {
        if (running) {
            return;
        }

        stopFlowFlag = false;

        for (InternalAction internalAction : internalActions) {
            if (internalAction.action == action) {
                runningAction = internalAction;
            }
        }

        if (runningAction == null) {
            return;
        }

        running = true;

        if (statuesListener != null) {
            statuesListener.onFlowStart();
        }

        flowLoop();
    }

    /**
     *
     */
    private Disposable timeoutDisposable;
    private Disposable delayDisposable;

    private void flowLoop() {

        if (stopFlowFlag) {
            running = false;

            if (stopFlowListener != null) {
                stopFlowListener.onStop();
            }
            if (statuesListener != null) {
                statuesListener.onFlowCancel();
            }

            return;
        }

        if ((runningAction == null) || (runningAction.action.getTag() == -1)) {  // 未添加映射的action
            running = false;
            if (statuesListener != null) {
                statuesListener.onFlowComplete();
            }
            return;
        }

        if (runningAction.actionCB == null) {
            runningAction.actionCB = new IActionCB() {
                @Override
                public synchronized void finish(Object obj) {
                    if (!this.getParent().canCB) {
                        return;
                    }
                    runningAction.canCB = false;

                    if (statuesListener != null) {  //  action结束状态回调
                        statuesListener.onActionFinish(runningAction.action);
                    }

                    // 停止超时定时器
                    if (timeoutDisposable != null) {
                        timeoutDisposable.dispose();
                        timeoutDisposable = null;
                    }

                    IAction action = runningAction.actionLink.nextAction(obj);  // 寻找下一个action
                    if (action == null) {
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

        // 配置了超时时间
        if ((runningAction.action.runParameters != null) && (runningAction.action.runParameters.timeout > 0)) {
            timeoutDisposable = Observable.timer(runningAction.action.runParameters.timeout, TimeUnit.MILLISECONDS).subscribe(new Consumer<Long>() {
                @Override
                public void accept(Long aLong) throws Exception {
                    timeoutDisposable = null;

                    runningAction.action.onTimeout();
                }
            });
        }

        // 开始执行任务
        final Observable<Object> objectObservable = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                runningAction.canCB = true;
                if (statuesListener != null) {
                    statuesListener.onActionStart(runningAction.action);
                }
                runningAction.action.onRun(runningAction.actionCB);
            }
        });


        // 设置调度线程
        if ((runningAction.action.runParameters != null) && (runningAction.action.runParameters.scheduler != null)) {
            objectObservable.subscribeOn(runningAction.action.runParameters.scheduler);
        }

//        final Observer observer = new Observer<Object>() {
//            @Override
//            public void onSubscribe(Disposable d) {
//
//            }
//
//            @Override
//            public void onNext(Object value) {
//
//            }
//
//            @Override
//            public void onError(Throwable e) {
//                runningAction.action.onCrash(e);
//            }
//
//            @Override
//            public void onComplete() {
//
//            }
//        };
        // 设置调度延迟时间
        if ((runningAction.action.runParameters != null) && (runningAction.action.runParameters.runDelay > 0)) {

            delayDisposable = Observable.timer(runningAction.action.runParameters.runDelay, TimeUnit.MILLISECONDS).subscribe(new Consumer<Long>() {
                @Override
                public void accept(Long aLong) throws Exception {
                    delayDisposable = null;
//                    objectObservable.subscribe(observer);
                    objectObservable.subscribe();
                }
            });
        } else {
            delayDisposable = null;
//            objectObservable.subscribe(observer);
            objectObservable.subscribe();
        }

    }

    /**
     * action
     *
     * @param <I> input params
     * @param <O> output params
     */
    public abstract static class IAction<I, O> {
        private int tag = -1;

        protected abstract void onRun(IActionCB<O> cb);

        protected abstract void onTimeout();

        public abstract void cancel();

//        protected abstract void onCrash(Throwable e);

        private I params;

        public void setParams(I params) {
            this.params = params;
        }

        public I getParams() {
            return params;
        }

        final int getTag() {
            return tag;
        }

        final void setTag(int tag) {
            this.tag = tag;
        }

        private RunParameters runParameters;

        public RunParameters getRunParameters() {
            return runParameters;
        }

        public void setRunParameters(RunParameters runParameters) {
            this.runParameters = runParameters;
        }
    }

    /**
     * action link
     *
     * @param <O> action output params type
     */
    public interface IActionLink<O> {
        IAction nextAction(O obj);
    }

    /**
     * action result
     *
     * @param <O> action output params type
     */
    public abstract static class IActionCB<O> {
        public abstract void finish(O obj);

        private InternalAction parent;

        InternalAction getParent() {
            return this.parent;
        }

        void setParent(InternalAction parent) {
            this.parent = parent;
        }
    }

    private static class InternalAction {
        IAction action;
        IActionLink actionLink;

        boolean canCB = false;

        IActionCB actionCB;
    }

    public static class RunParameters {
        private Scheduler scheduler;
        private long timeout = 0;
        private long runDelay = 0;

        public RunParameters setScheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public RunParameters setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Scheduler getScheduler() {
            return scheduler;
        }

        public long getTimeout() {
            return timeout;
        }

        public long getRunDelay() {
            return runDelay;
        }

        public RunParameters setRunDelay(long runDelay) {
            this.runDelay = runDelay;
            return this;
        }
    }

    /**
     * cancel flow
     */
    private boolean stopFlowFlag = false;
    private StopFlowListener stopFlowListener;

    public void cancelFlow(StopFlowListener listener) {

        stopFlowListener = listener;
        stopFlowFlag = true;

        if (delayDisposable != null) {
            delayDisposable.dispose();
            flowLoop();
        }

        if (runningAction != null) {
            runningAction.action.cancel();
        }

    }

    public interface StopFlowListener {
        void onStop();
    }

    /**
     * StatuesListener
     */
    private StatuesListener statuesListener;

    public void setStatuesListener(StatuesListener statuesListener) {
        this.statuesListener = statuesListener;
    }

    public interface StatuesListener {
        void onFlowStart();

        void onFlowComplete();

        void onFlowCancel();

        void onActionStart(IAction action);

        void onActionFinish(IAction action);
    }

    public interface ActionRing<O> {
        void onStart();

        O onProcess();

        void onFinish();
    }

    /**
     * 判断流程是否真正进行
     *
     * @return
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 获取正在运行的action
     *
     * @return
     */
    public IAction getRunningAction() {
        if (!isRunning()) {
            return null;
        }
        return runningAction.action;
    }

}
