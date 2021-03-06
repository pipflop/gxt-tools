/*
* Copyright (c) 2008 TouK.pl
* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package pl.touk.wonderfulsecurity.gwt.client.ui;


import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;

import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import pl.touk.wonderfulsecurity.beans.PagedQueryResult;
import pl.touk.wonderfulsecurity.gwt.client.model.PagedQueryResultReader;
import pl.touk.tola.gwt.client.widgets.grid.VariablePageSizePagingToolBar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * TODO: by rpt to trzeba przeniesc z tego pakietu. Tabelka jest w tyms amym pakeicie co logger ? w ogole co to robi w security ? oznaczam jako deprecated. Tym bardziej, ze ulepszylismy implementacje
 *
 * @author Lukasz Kucharski - lkc@touk.pl
 */
@Deprecated
public abstract class BasePagedList extends ContentPanel {
// ------------------------------ FIELDS ------------------------------

    protected BasePagingLoader pagingLoader;
    protected PagingToolBar pagingToolbar;
    protected String expandedColumnId;
    protected Class readerBeanFactoryClass;
    protected Grid grid;
    protected String uniqueName;
    //To jest pole, po ktorym poznajemy, ktory wiersz podswietlic po zaznaczeniu
    protected String selectionId = "id";

// --------------------------- CONSTRUCTORS ---------------------------

    protected BasePagedList(Class clazz, String uniqueName) {
        this.uniqueName = uniqueName;
        this.readerBeanFactoryClass = clazz;

        this.setLayout(new FitLayout());
        this.setFrame(true);
        this.setHeading(buildHeading());

        RpcProxy proxy = afterLoadSelectionProxy(constructRpcProxy());

        if (readerBeanFactoryClass == null) {
            pagingLoader = new BasePagingLoader(proxy, new PagedQueryResultReader());
        } else {
            pagingLoader = new BasePagingLoader(proxy, new PagedQueryResultReader(readerBeanFactoryClass));
        }
        pagingLoader.setRemoteSort(true);
        ListStore listStore = new ListStore(pagingLoader);

        ColumnModel cm = buildColumnModel();

        grid = new Grid(listStore, cm);
        grid.disableEvents(false);
        grid.setBorders(true);
        grid.setAutoExpandMax(800);
        grid.setAutoExpandColumn(expandedColumnId);

        grid.addListener(Events.RowDoubleClick, new Listener<GridEvent>() {
            public void handleEvent(GridEvent ge) {
                afterGridRowDoubleClicked(ge);
            }
        });

        for (ComponentPlugin plugin : buildGridPlugins()) {
            grid.addPlugin(plugin);
        }


        pagingToolbar = new VariablePageSizePagingToolBar(uniqueName);
        pagingToolbar.bind(pagingLoader);
        this.setBottomComponent(pagingToolbar);

        //dodałem to, bo po zmianie ilosci rekordów w pageowaniu a nastepnie przesortowaniu był bug.
        pagingLoader.addLoadListener(new LoadListener() {

            @Override
            public void loaderBeforeLoad(LoadEvent le) {
                super.loaderBeforeLoad(le);
                if (pagingToolbar != null) {
                    pagingLoader.setLimit(pagingToolbar.getPageSize());
                }
            }
        });

        Component top = constructTopComponent();
        if (top != null) {
            this.setTopComponent(top);
        }

        add(grid);

    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getExpandedColumnId() {
        return expandedColumnId;
    }

    public void setExpandedColumnId(String expandedColumnId) {
        this.expandedColumnId = expandedColumnId;
    }

    public BasePagingLoader getPagingLoader() {
        return pagingLoader;
    }

    // -------------------------- OTHER METHODS --------------------------

    @Override
    protected void afterRender() {
        super.afterRender();


        pagingLoader.load();
    }

    protected void beforeRender() {
        super.beforeRender();


    }

    private RpcProxy afterLoadSelectionProxy(final RpcProxy proxy) {
        return new RpcProxy() {
            @Override
            protected void load(Object o, AsyncCallback asyncCallback) {
                proxy.load(null, o, selectionCallback(asyncCallback));
            }
        };
    }

    private AsyncCallback selectionCallback(final AsyncCallback asyncCallback) {
        com.extjs.gxt.ui.client.data.ModelData selected = grid.getSelectionModel().getSelectedItem();
        final Object currentlySelected = selected == null ? null : selected.get(selectionId);

        return new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                asyncCallback.onFailure(throwable);
            }

            public void onSuccess(Object arrayListPagedQueryResult) {
                asyncCallback.onSuccess(arrayListPagedQueryResult);
                if (currentlySelected != null) {
                    selectById(currentlySelected);
                }
            }
        };
    }

    private void selectById(Object id) {
        for (int i = 0; i < grid.getStore().getCount(); i++) {
            if (id.equals(grid.getStore().getAt(i).get(selectionId))) {
                grid.getSelectionModel().select(i, false);
            }
        }
    }

    protected List<ComponentPlugin> buildGridPlugins() {
        return Collections.EMPTY_LIST;
    }

    protected abstract String buildHeading();

    protected abstract RpcProxy constructRpcProxy();

    protected abstract ColumnModel buildColumnModel();

    protected abstract void afterGridRowDoubleClicked(GridEvent ge);

    protected Component constructTopComponent() {
        return null;
    }
}
