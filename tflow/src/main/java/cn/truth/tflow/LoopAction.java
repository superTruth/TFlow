package cn.truth.tflow;

public abstract class LoopAction<I, O, MIDI, MIDO> extends TFlow.IAction<I, O> {

    private TFlow tFlow;
    private TFlow.IAction<MIDI, MIDO> action;

    public LoopAction(TFlow.IAction<MIDI, MIDO> action) {
        this.action = action;
    }

    private TFlow.IActionCB<O> cb;
    @Override
    protected void onRun(final TFlow.IActionCB<O> cb) {
        this.cb = cb;
        tFlow = new TFlow();

        // 获取第一个事件
        MIDI event = pickOneEvent();
        if(event == null){
            cb.finish(getFinalRet());
            return;
        }
        action.setParams(event);

        tFlow.addAction(action, new TFlow.IActionLink<MIDO>() {
            @Override
            public TFlow.IAction nextAction(MIDO obj) {
                setOneRet(obj);

                MIDI event = pickOneEvent();
                if (event == null){
                    cb.finish(getFinalRet());
                    return null;
                }

                // 继续执行loop
                action.setParams(event);

                return action;
            }
        });

        tFlow.startFlow(action);
    }


    // 流程启动
    protected abstract void loopStart();

    // 获取最终结果
    protected abstract O getFinalRet();

    // 设置一个运行事件结果
    protected abstract void setOneRet(MIDO out);

    // 获取一个运行事件
    protected abstract MIDI pickOneEvent();

}