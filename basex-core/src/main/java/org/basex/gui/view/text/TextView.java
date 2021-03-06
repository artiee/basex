package org.basex.gui.view.text;

import static org.basex.core.Text.*;
import static org.basex.gui.GUIConstants.*;
import static org.basex.util.Token.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import org.basex.core.*;
import org.basex.core.parse.*;
import org.basex.gui.*;
import org.basex.gui.layout.*;
import org.basex.gui.layout.BaseXFileChooser.Mode;
import org.basex.gui.text.*;
import org.basex.gui.view.*;
import org.basex.io.*;
import org.basex.io.out.*;
import org.basex.io.serial.*;
import org.basex.query.*;
import org.basex.query.value.*;
import org.basex.query.value.seq.*;
import org.basex.util.*;

/**
 * This class offers a fast text view, using the {@link TextPanel} class.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class TextView extends View {
  /** Search editor. */
  private final SearchEditor search;

  /** Header string. */
  private final BaseXHeader header;
  /** Home button. */
  private final AbstractButton home;
  /** Text Area. */
  private final TextPanel text;

  /** Cached command. */
  private Command cachedCmd;
  /** Cached nodes. */
  private DBNodes cachedNodes;

  /**
   * Default constructor.
   * @param man view manager
   */
  public TextView(final ViewNotifier man) {
    super(TEXTVIEW, man);
    border(5).layout(new BorderLayout(0, 5));

    header = new BaseXHeader(RESULT);

    home = BaseXButton.command(GUIMenuCmd.C_HOME, gui);
    home.setEnabled(false);

    text = new TextPanel(false, gui);
    text.setSyntax(new SyntaxXML());
    search = new SearchEditor(gui, text);

    final AbstractButton save = BaseXButton.get("c_save", SAVE, false, gui);
    final AbstractButton find = search.button(FIND);

    final BaseXBack buttons = new BaseXBack(false);
    buttons.layout(new TableLayout(1, 3, 1, 0)).border(0, 0, 4, 0);
    buttons.add(save);
    buttons.add(home);
    buttons.add(find);

    final BaseXBack b = new BaseXBack(false).layout(new BorderLayout());
    b.add(buttons, BorderLayout.WEST);
    b.add(header, BorderLayout.EAST);
    add(b, BorderLayout.NORTH);

    add(search, BorderLayout.CENTER);

    save.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        save();
      }
    });
    refreshLayout();
  }

  @Override
  public void refreshInit() {
    refreshContext(true, true);
  }

  @Override
  public void refreshFocus() {
  }

  @Override
  public void refreshMark() {
    setText(gui.context.marked);
  }

  @Override
  public void refreshContext(final boolean more, final boolean quick) {
    setText(gui.context.current());
  }

  @Override
  public void refreshLayout() {
    header.refreshLayout();
    text.setFont(mfont);
    search.bar().refreshLayout();
  }

  @Override
  public void refreshUpdate() {
    refreshContext(true, true);
  }

  @Override
  public boolean visible() {
    return gui.gopts.get(GUIOptions.SHOWTEXT);
  }

  @Override
  public void visible(final boolean v) {
    gui.gopts.set(GUIOptions.SHOWTEXT, v);
  }

  @Override
  protected boolean db() {
    return false;
  }

  /**
   * Serializes the specified nodes.
   * @param nodes nodes to display
   */
  private void setText(final DBNodes nodes) {
    if(visible()) {
      try {
        final ArrayOutput ao = new ArrayOutput();
        ao.setLimit(gui.gopts.get(GUIOptions.MAXTEXT));
        if(nodes != null) nodes.serialize(Serializer.get(ao));
        setText(ao);
        cachedCmd = null;
        cachedNodes = ao.finished() ? nodes : null;
      } catch(final IOException ex) {
        Util.debug(ex);
      }
    } else {
      home.setEnabled(gui.context.data() != null);
    }
  }

  /**
   * Caches the output, or indicates that the query must be executed again in order to retrieve
   * the full result.
   * @param out cached output
   * @param command command
   * @param result result (can be {@code null})
   * @throws QueryException query exception
   */
  public void cache(final ArrayOutput out, final Command command, final Value result)
      throws QueryException {

    // cache command or node set
    cachedCmd = null;
    cachedNodes = null;

    final int max = gui.gopts.get(GUIOptions.MAXRESULTS);
    boolean cacheCmd = false;
    if(result != null && result.size() >= max) {
      // result was larger than number of retrieved result items: create new command instance
      cacheCmd = true;
    } else if(out.finished()) {
      // cache is exhausted... cache node set, or create new command instance
      if(result instanceof DBNodes) cachedNodes = (DBNodes) result;
      else cacheCmd = true;
    }
    // otherwise, the displayed text and the cached result are equal

    // create new command instance
    if(cacheCmd) {
      cachedCmd = CommandParser.get(command.toString(), gui.context).parseSingle();
      cachedCmd.baseURI(command.baseURI());
    }
  }

  /**
   * Sets the output text.
   * @param out cached output
   */
  public void setText(final ArrayOutput out) {
    final byte[] buf = out.buffer();
    final int size = (int) out.size();
    final byte[] chop = token(DOTS);
    if(out.finished() && size >= chop.length) {
      System.arraycopy(chop, 0, buf, size - chop.length, chop.length);
    }
    text.setText(buf, size);
    header.setText((out.finished() ? CHOPPED : "") + RESULT);
    home.setEnabled(gui.context.data() != null);
  }

  /**
   * Saves the displayed text.
   */
  private void save() {
    final BaseXFileChooser fc = new BaseXFileChooser(SAVE_AS,
        gui.gopts.get(GUIOptions.WORKPATH), gui).suffix(IO.XMLSUFFIX);

    final IO file = fc.select(Mode.FSAVE);
    if(file == null) return;
    gui.gopts.set(GUIOptions.WORKPATH, file.path());

    gui.cursor(CURSORWAIT, true);
    try(PrintOutput out = new PrintOutput(file.toString())) {
      if(cachedCmd != null) {
        cachedCmd.execute(gui.context, out);
      } else if(cachedNodes != null) {
        cachedNodes.serialize(Serializer.get(out));
      } else {
        out.write(text.getText());
      }
    } catch(final IOException ex) {
      BaseXDialog.error(gui, Util.info(FILE_NOT_SAVED_X, file));
    } finally {
      gui.cursor(CURSORARROW, true);
    }
  }
}
