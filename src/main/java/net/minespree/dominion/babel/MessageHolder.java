package net.minespree.dominion.babel;

import com.google.common.collect.Table;

/**
 * @since 01/09/2017
 */
public interface MessageHolder {

    Table<SupportedLanguage, String, String> getMessageTable();

}
