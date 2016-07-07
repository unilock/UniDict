package wanion.unidict;

/*
 * Created by WanionCane(https://github.com/WanionCane).
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 1.1. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/1.1/.
 */

import mezz.jei.api.*;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@JEIPlugin
public class UniJEIPlugin implements IModPlugin
{
    private static final List<ItemStack> stacksToHideList = new ArrayList<>();
    private IItemBlacklist itemBlackList;

    public static void hide(final ItemStack itemStack)
    {
        stacksToHideList.add(itemStack);
    }

    @Override
    public void register(@Nonnull final IModRegistry iModRegistry)
    {
        itemBlackList = iModRegistry.getJeiHelpers().getItemBlacklist();
    }

    @Override
    public void onRuntimeAvailable(@Nonnull final IJeiRuntime iJeiRuntime)
    {
        stacksToHideList.forEach(itemBlackList::addItemToBlacklist);
    }
}