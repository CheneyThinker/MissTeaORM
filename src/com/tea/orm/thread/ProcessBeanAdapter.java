package com.tea.orm.thread;

import java.util.List;

public final class ProcessBeanAdapter {
	
	private PromptDialog promptDialog;
	
	public ProcessBeanAdapter() {
		promptDialog = new PromptDialog();
	}
	
	public <T, U> void addBeanAdapter(final BeanAdapter adapter) {
		new Thread(new Runnable() {

			public void run() {
				try {
					U[][] complex = adapter.getComplex();
					U[] simple = adapter.getSimple();
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
