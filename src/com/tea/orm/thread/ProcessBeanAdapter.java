package com.tea.orm.thread;

import java.util.List;

public final class ProcessBeanAdapter {
	
	private PromptDialog promptDialog;
	
	public ProcessBeanAdapter() {
		promptDialog = new PromptDialog();
	}
	
	public <T> void addBeanAdapter(final BeanAdapter<T> adapter) {
		new Thread(new Runnable() {

			public void run() {
				try {
					T[][] complex = adapter.getComplex();
					T[] simple = adapter.getSimple();
					List<T> lists = adapter.getLists();
					promptDialog.dispose();
					if(complex != null)
						adapter.fill(complex);
					if (simple != null)
						adapter.fill(simple);
					if (lists != null)
						adapter.fill(lists);
					if (complex == null && simple == null && lists == null)
						adapter.failure();
				} catch (Exception e) {
					promptDialog.dispose();
				}
			}
		}).start();
	}
	
}
