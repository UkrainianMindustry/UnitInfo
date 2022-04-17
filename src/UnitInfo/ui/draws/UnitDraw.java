package UnitInfo.ui.draws;

import UnitInfo.ui.FreeBar;
import arc.Core;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.CheckBox;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.ai.Pathfinder;
import mindustry.ai.types.*;
import mindustry.entities.units.UnitCommand;
import mindustry.entities.units.UnitController;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.logic.LUnitControl;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.CommandCenter;

import java.util.Objects;

import static UnitInfo.core.OverDrawer.isInCamera;
import static UnitInfo.core.OverDrawer.isOutCamera;
import static mindustry.Vars.*;

public class UnitDraw extends OverDraw {
    Seq<Tile> pathTiles = new Seq<>();
    int otherCores;
    boolean pathLine = false, unitLine = false, logicLine = false, bar = false, item = false;

    UnitDraw(String name, TextureRegionDrawable icon) {
        super(name, icon);
    }

    @Override
    public void draw() {
        if(!enabled) return;

        Groups.unit.each(u-> isInCamera(u.x, u.y, u.hitSize), u -> {
            UnitController c = u.controller();
            UnitCommand com = u.team.data().command;

            if(logicLine && c instanceof LogicAI ai && (ai.control == LUnitControl.approach || ai.control == LUnitControl.move)) {
                Lines.stroke(1, u.team.color);
                Lines.line(u.x(), u.y(), ai.moveX, ai.moveY);
                Lines.stroke(0.5f + Mathf.absin(6f, 0.5f), Tmp.c1.set(Pal.logicOperations).lerp(Pal.sap, Mathf.absin(6f, 0.5f)));
                Lines.line(u.x(), u.y(), ai.controller.x, ai.controller.y);
            }

            if(unitLine && !u.type.flying && com != UnitCommand.idle && !(c instanceof MinerAI || c instanceof BuilderAI || c instanceof RepairAI || c instanceof DefenderAI || c instanceof FormationAI || c instanceof FlyingAI)) {
                Lines.stroke(1, u.team.color);

                otherCores = Groups.build.count(b -> b instanceof CoreBlock.CoreBuild && b.team != u.team);
                pathTiles.clear();
                getNextTile(u.tileOn(), u.controller() instanceof SuicideAI ? 0 : u.pathType(), u.team, com.ordinal());
                pathTiles.filter(Objects::nonNull);
                for(int i = 0; i < pathTiles.size-1; i++) {
                    Tile from = pathTiles.get(i);
                    Tile to = pathTiles.get(i + 1);
                    if(isOutCamera(from.worldx(), from.worldy())) continue;
                    Lines.line(from.worldx(), from.worldy(), to.worldx(), to.worldy());
                }
            }

            if(bar) FreeBar.draw(u);

            if(item && !renderer.pixelator.enabled() && u.item() != null && u.itemTime > 0.01f)
                Fonts.outline.draw(u.stack.amount + "",
                        u.x + Angles.trnsx(u.rotation + 180f, u.type.itemOffsetY),
                        u.y + Angles.trnsy(u.rotation + 180f, u.type.itemOffsetY) - 3,
                        Pal.accent, 0.25f * u.itemTime / Scl.scl(1f), false, Align.center);
        });

        if(pathLine) spawner.getSpawns().each(t -> {
            Team enemyTeam = state.rules.waveTeam;
            Lines.stroke(1, enemyTeam.color);
            for(int p = 0; p < (Vars.state.rules.spawns.count(g->g.type.naval)>0?3:2); p++) {
                pathTiles.clear();
                otherCores = Groups.build.count(b -> b instanceof CoreBlock.CoreBuild && b.team != enemyTeam);
                getNextTile(t, p, enemyTeam, Pathfinder.fieldCore);
                pathTiles.filter(Objects::nonNull);

                for(int i = 0; i < pathTiles.size-1; i++) {
                    Tile from = pathTiles.get(i);
                    Tile to = pathTiles.get(i + 1);
                    if(isOutCamera(from.worldx(), from.worldy())) continue;
                    Lines.line(from.worldx(), from.worldy(), to.worldx(), to.worldy());
                }
        }
        });
    }

    Tile getNextTile(Tile tile, int cost, Team team, int finder) {
        Pathfinder.Flowfield field = pathfinder.getField(team, cost, Mathf.clamp(finder, 0, 1));
        Tile tile1 = pathfinder.getTargetTile(tile, field);
        pathTiles.add(tile1);
        if(tile1 == tile || tile1 == null ||
                (finder == 0 && (otherCores != Groups.build.count(b -> b instanceof CoreBlock.CoreBuild && b.team != team) || tile1.build instanceof CoreBlock.CoreBuild)) ||
                (finder == 1 && tile1.build instanceof CommandCenter.CommandBuild))
            return tile1;
        return getNextTile(tile1, cost, team, finder);
    }

    @Override
    public void displayStats(Table parent) {
        super.displayStats(parent);

        parent.background(Styles.squaret.up);

        parent.check("enable path line", pathLine&&enabled, b->pathLine=b&&enabled).disabled(!enabled).row();
        parent.check("enable logic line", logicLine&&enabled, b->logicLine=b&&enabled).disabled(!enabled).row();
        parent.check("enable unit line", unitLine&&enabled, b->unitLine=b&&enabled).disabled(!enabled).row();
        parent.check("enable unit item", item&&enabled, b->item=b&&enabled).disabled(!enabled).row();
        parent.check("enable unit bar", bar&&enabled, b->bar=b&&enabled).disabled(!enabled).row();
    }

    @Override
    public <T> void onEnabled(T param) {
        super.onEnabled(param);

        if(param instanceof Table t) {
            for (int i = 0; i < t.getChildren().size; i++) {
                Element elem = t.getChildren().get(i);
                if (elem instanceof CheckBox cb) cb.setDisabled(!enabled);
            }
        }
    }
}