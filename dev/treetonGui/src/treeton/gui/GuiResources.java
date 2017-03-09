/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import treeton.core.config.BasicConfiguration;
import treeton.gui.util.FileDialogUtil;
import treeton.gui.util.ToolBarFactory;

import javax.swing.*;
import java.awt.*;

public class GuiResources {
    public static Icon iconVoid;
    public static Icon iconOwner;
    public static Icon iconOwnerComment;
    public static Icon iconComment;
    public static Icon iconConnect;
    public static Icon iconLockedComment;
    public static Icon iconLocked;
    public static Icon iconNew;

    public static Icon iconDictVoid;
    public static Icon iconDictLockedByOther;
    public static Icon iconDictLockedByMe;
    public static Icon iconDictLockedIndirectly;
    public static Icon iconDictLockUnavailable;

    public static Icon iconBase;
    public static Icon iconDepends;
    public static Icon iconNone;
    public static Icon iconNoneStrike;
    public static Icon iconNoneRef;
    public static Icon iconResChain;
    public static Icon iconResChainRef;

    public static Icon iconFolderPlu;
    public static Icon iconFolderMin;

    public static Icon iconExpand;
    public static Icon iconCollapse;
    public static Icon iconDatabase;
    public static Icon iconLock12;
    public static Icon iconLockR12;
    public static Icon iconMark12;
    public static Icon iconInfo12;
    public static Icon iconAlert12;

    public static Icon iconBulbRed;
    public static Icon iconBulbGreen;
    public static Icon iconBulbYellow;

    public static Icon iconExcpOn;
    public static Icon iconExcpOnStrike;
    public static Icon iconExcpOff;

    public static Icon iconFolderGreen;

    public static Icon iconRun;
    public static Icon iconStopProcess;

    public static Icon iconAggregateSyntaxRule;
    public static Icon iconSimpleSyntaxRule;
    public static Icon iconTokenSyntaxRule;
    public static Icon iconRootSyntaxRule;
    public static Icon iconFolderAnyItem;
    public static Icon iconFolderAnyItemDis;

    public static Icon iconAggregateStrong;
    public static Icon iconAggregateWeak;
    public static Icon iconAddMemberStrong;
    public static Icon iconAddMemberWeak;
    public static Icon iconLink;
    public static Icon iconRemoveLink;
    public static Icon iconRemoveAggregate;

    public static Icon iconEditFile;
    public static Icon iconPlus;

    public static Icon iconDeleteSyntaxRuleUsage;

    public static Icon iconLegendSmall;
    public static Icon iconTraceMove;
    public static Icon iconTraceMoveUnknown;
    public static Icon iconStop;
    public static Icon iconSearch;
    public static Icon iconTable;

    public static Icon iconBugRed;
    public static Icon iconBugGreen;
    public static Icon iconBugYellow;

    public static Icon iconWarning;

    public static Image imageLock;
    public static Image imagePlug;
    public static Image imageSocket;

    public static ToolBarFactory tbf;
    public static FileDialogUtil fileDialogUtil;

    static {
        BasicConfiguration.getResource("/gui/rec-v.gif");

        iconVoid = new ImageIcon(BasicConfiguration.getResource("/gui/rec-v.gif"));
        iconOwner = new ImageIcon(BasicConfiguration.getResource("/gui/rec-o.gif"));
        iconOwnerComment = new ImageIcon(BasicConfiguration.getResource("/gui/rec-oc.gif"));
        iconComment = new ImageIcon(BasicConfiguration.getResource("/gui/rec-c.gif"));
        iconConnect = new ImageIcon(BasicConfiguration.getResource("/gui/connect.gif"));
        iconLockedComment = new ImageIcon(BasicConfiguration.getResource("/gui/rec-lc.gif"));
        iconLocked = new ImageIcon(BasicConfiguration.getResource("/gui/rec-l.gif"));
        iconNew = new ImageIcon(BasicConfiguration.getResource("/gui/rec-n.gif"));
        iconExcpOn = new ImageIcon(BasicConfiguration.getResource("/gui/excp-on.gif"));
        iconExcpOnStrike = new ImageIcon(BasicConfiguration.getResource("/gui/excp-onStrike.gif"));
        iconExcpOff = new ImageIcon(BasicConfiguration.getResource("/gui/excp-off.gif"));
        iconDictVoid = new ImageIcon(BasicConfiguration.getResource("/gui/rec-v.gif"));
        iconDictLockedByOther = new ImageIcon(BasicConfiguration.getResource("/gui/dic-lo.gif"));
        iconDictLockedByMe = new ImageIcon(BasicConfiguration.getResource("/gui/dic-lm.gif"));
        iconDictLockedIndirectly = new ImageIcon(BasicConfiguration.getResource("/gui/dic-li.gif"));
        iconDictLockUnavailable = new ImageIcon(BasicConfiguration.getResource("/gui/dic-u.gif"));
        iconBase = new ImageIcon(BasicConfiguration.getResource("/gui/base.gif"));
        iconDepends = new ImageIcon(BasicConfiguration.getResource("/gui/depn.gif"));
        iconNone = new ImageIcon(BasicConfiguration.getResource("/gui/none.gif"));
        iconNoneStrike = new ImageIcon(BasicConfiguration.getResource("/gui/noneStrike.gif"));
        iconNoneRef = new ImageIcon(BasicConfiguration.getResource("/gui/noneRef.gif"));
        iconResChain = new ImageIcon(BasicConfiguration.getResource("/gui/chain.gif"));
        iconResChainRef = new ImageIcon(BasicConfiguration.getResource("/gui/chainRef.gif"));
        iconFolderPlu = new ImageIcon(BasicConfiguration.getResource("/gui/foldplu.gif"));
        iconFolderMin = new ImageIcon(BasicConfiguration.getResource("/gui/foldmin.gif"));
        iconExpand = new ImageIcon(BasicConfiguration.getResource("/gui/expand.gif"));
        iconCollapse = new ImageIcon(BasicConfiguration.getResource("/gui/collapse.gif"));
        iconDatabase = new ImageIcon(BasicConfiguration.getResource("/gui/database.gif"));
        iconLock12 = new ImageIcon(BasicConfiguration.getResource("/gui/reclock.gif"));
        iconLockR12 = new ImageIcon(BasicConfiguration.getResource("/gui/reclockr.gif"));
        iconMark12 = new ImageIcon(BasicConfiguration.getResource("/gui/mark12.gif"));
        iconInfo12 = new ImageIcon(BasicConfiguration.getResource("/gui/info12.gif"));
        iconAlert12 = new ImageIcon(BasicConfiguration.getResource("/gui/alert12.gif"));
        iconBulbRed = new ImageIcon(BasicConfiguration.getResource("/gui/bulbRed.gif"));
        iconBulbGreen = new ImageIcon(BasicConfiguration.getResource("/gui/bulbGreen.gif"));
        iconBulbYellow = new ImageIcon(BasicConfiguration.getResource("/gui/bulbYellow.gif"));
        iconRun = new ImageIcon(BasicConfiguration.getResource("/gui/run.gif"));
        iconStopProcess = new ImageIcon(BasicConfiguration.getResource("/gui/stopProcess.gif"));
        iconAggregateSyntaxRule = new ImageIcon(BasicConfiguration.getResource("/gui/agro.gif"));
        iconSimpleSyntaxRule = new ImageIcon(BasicConfiguration.getResource("/gui/srule.gif"));
        iconTokenSyntaxRule = new ImageIcon(BasicConfiguration.getResource("/gui/tokrule.gif"));
        iconRootSyntaxRule = new ImageIcon(BasicConfiguration.getResource("/gui/rootrule.gif"));
        iconDeleteSyntaxRuleUsage = new ImageIcon(BasicConfiguration.getResource("/gui/delusage.gif"));
        iconLegendSmall = new ImageIcon(BasicConfiguration.getResource("/gui/legendSmall.gif"));
        iconTraceMove = new ImageIcon(BasicConfiguration.getResource("/gui/tracemove.gif"));
        iconTraceMoveUnknown = new ImageIcon(BasicConfiguration.getResource("/gui/tracemoveUnknown.gif"));
        iconStop = new ImageIcon(BasicConfiguration.getResource("/gui/stop.gif"));
        iconSearch = new ImageIcon(BasicConfiguration.getResource("/gui/search.gif"));
        iconTable = new ImageIcon(BasicConfiguration.getResource("/gui/table.gif"));
        iconFolderGreen = new ImageIcon(BasicConfiguration.getResource("/gui/foldergreen.gif"));
        iconFolderAnyItem = new ImageIcon(BasicConfiguration.getResource("/gui/project.gif"));
        iconFolderAnyItemDis = new ImageIcon(BasicConfiguration.getResource("/gui/project-dis.gif"));

        iconAggregateStrong = new ImageIcon(BasicConfiguration.getResource("/gui/aggregateStrong.gif"));
        iconAggregateWeak = new ImageIcon(BasicConfiguration.getResource("/gui/aggregateWeak.gif"));
        iconAddMemberStrong = new ImageIcon(BasicConfiguration.getResource("/gui/addMemberStrong.gif"));
        iconAddMemberWeak = new ImageIcon(BasicConfiguration.getResource("/gui/addMemberWeak.gif"));
        iconLink = new ImageIcon(BasicConfiguration.getResource("/gui/link.gif"));

        iconRemoveLink = new ImageIcon(BasicConfiguration.getResource("/gui/removeLink.gif"));
        iconRemoveAggregate = new ImageIcon(BasicConfiguration.getResource("/gui/removeAggregate.gif"));

        iconEditFile = new ImageIcon(BasicConfiguration.getResource("/gui/editfile.gif"));
        iconPlus = new ImageIcon(BasicConfiguration.getResource("/gui/add12.gif"));

        iconBugRed = new ImageIcon(BasicConfiguration.getResource("/gui/bug_red.png"));
        iconBugGreen = new ImageIcon(BasicConfiguration.getResource("/gui/bug_green.png"));
        iconBugYellow = new ImageIcon(BasicConfiguration.getResource("/gui/bug_yellow.png"));

        iconWarning = new ImageIcon(BasicConfiguration.getResource("/gui/warning.png"));

        imageLock = Toolkit.getDefaultToolkit().getImage(BasicConfiguration.getResource("/gui/reclock.gif"));
        imageLock.getWidth(null);
        imagePlug = Toolkit.getDefaultToolkit().getImage(BasicConfiguration.getResource("/gui/plug.gif"));
        imagePlug.getWidth(null);
        imageSocket = Toolkit.getDefaultToolkit().getImage(BasicConfiguration.getResource("/gui/socket.gif"));
        imageSocket.getWidth(null);


        tbf = new ToolBarFactory();
        fileDialogUtil = new FileDialogUtil(
                TreetonSessionProperties.getInstance().getString("Dir.last",
                        System.getProperties().getProperty("user.dir"), true));
    }

    public static ImageIcon getImageIcon(String imgFile) {
        return new ImageIcon(BasicConfiguration.getResource("/gui/" + imgFile));
    }
}
