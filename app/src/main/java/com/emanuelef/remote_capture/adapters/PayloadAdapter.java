/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.emanuelef.remote_capture.CaptureService;
import com.emanuelef.remote_capture.HTTPReassembly;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.model.ConnectionDescriptor;
import com.emanuelef.remote_capture.model.PayloadChunk;
import com.emanuelef.remote_capture.model.PayloadChunk.ChunkType;
import com.emanuelef.remote_capture.model.Prefs;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/* An adapter to show PayloadChunk items.
 * Each item is wrapped into an AdapterChunk. An item can either be collapsed or expanded.
 * Since the text of a chunk can be very long (hundreds of KB) and rendering it would freeze the UI,
 * it is split into pages of VISUAL_PAGE_SIZE. */
public class PayloadAdapter extends RecyclerView.Adapter<PayloadAdapter.PayloadViewHolder> implements HTTPReassembly.ReassemblyListener {
    private static final String TAG = "PayloadAdapter";
    public static final int COLLAPSE_CHUNK_SIZE = 1500;
    public static final int VISUAL_PAGE_SIZE = 4020; // must be a multiple of 67 to avoid splitting the hexdump
    private final LayoutInflater mLayoutInflater;
    private final ConnectionDescriptor mConn;
    private final Context mContext;
    private final ChunkType mMode;
    private int mHandledChunks;
    private AdapterChunk mUnrepliedHttpReq = null;
    private final ArrayList<AdapterChunk> mChunks = new ArrayList<>();
    private final HTTPReassembly mHttpReq;
    private final HTTPReassembly mHttpRes;
    private boolean mShowAsPrintable;

    public PayloadAdapter(Context context, ConnectionDescriptor conn, ChunkType mode) {
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mConn = conn;
        mContext = context;
        mMode = mode;

        // Note: in minimal mode, only the first chunk is captured, so don't reassemble them
        boolean reassemble = (CaptureService.getCurPayloadMode() == Prefs.PayloadMode.FULL);

        // each direction must have its separate reassembly
        mHttpReq = new HTTPReassembly(reassemble, this);
        mHttpRes = new HTTPReassembly(reassemble, this);

        handleChunksAdded(mConn.getNumPayloadChunks());
    }

    private class AdapterChunk {
        private final PayloadChunk mChunk;
        private String mTheText;
        private boolean mIsExpanded;
        private int mNumPages = 1;
        public final int incrId;

        AdapterChunk(PayloadChunk _chunk, int incr_id) {
            mChunk = _chunk;
            incrId = incr_id;
        }

        boolean canBeExpanded() {
            return mChunk.payload.length > COLLAPSE_CHUNK_SIZE;
        }

        boolean isExpanded() {
            return mIsExpanded;
        }

        int getNumPages() {
            return mNumPages;
        }

        PayloadChunk getPayloadChunk() {
            return mChunk;
        }

        private void makeText() {
            int dump_len = mIsExpanded ? mChunk.payload.length : Math.min(mChunk.payload.length, COLLAPSE_CHUNK_SIZE);

            if(!mShowAsPrintable)
                mTheText = Utils.hexdump(mChunk.payload, 0, dump_len);
            else
                mTheText = new String(mChunk.payload, 0, dump_len, StandardCharsets.UTF_8);
        }

        void expand() {
            mIsExpanded = true;
            makeText();

            // round up div
            mNumPages = (mTheText.length() + VISUAL_PAGE_SIZE - 1) / VISUAL_PAGE_SIZE;
        }

        // collapses the item and returns the old number of pages
        void collapse() {
            mIsExpanded = false;
            mTheText = null;

            mNumPages = 1;
        }

        String getText(int start, int end) {
            if(mTheText == null)
                makeText();

            if((start == 0) && (end >= mTheText.length() - 1)) {
                return mTheText;
            }

            return mTheText.substring(start, end);
        }

        Page getPage(int pageIdx) {
            assert(pageIdx < mNumPages);

            if(mTheText == null)
                makeText();

            if(!mIsExpanded)
                return new Page(this, 0, mTheText.length() - 1, true);
            else
                return new Page(this, pageIdx * VISUAL_PAGE_SIZE,
                        Math.min(((pageIdx + 1) * VISUAL_PAGE_SIZE) - 1, mTheText.length() - 1),
                        pageIdx == (mNumPages - 1));
        }
    }

    private static class Page {
        AdapterChunk adaptChunk;
        int textStart;
        int textEnd;
        boolean isLast;

        Page(AdapterChunk _adaptChunk, int _textStart, int _textEnd, boolean _isLast) {
            adaptChunk = _adaptChunk;
            textStart = _textStart;
            textEnd = _textEnd;
            isLast = _isLast;
        }

        boolean isFirst() {
            return (textStart == 0);
        }

        String getText() {
            return adaptChunk.getText(textStart, textEnd);
        }
    }

    protected static class PayloadViewHolder extends RecyclerView.ViewHolder {
        View headerLine;
        View dumpBox;
        TextView header;
        TextView dump;
        TextView contentType;
        ImageView expandButton;

        public PayloadViewHolder(View view) {
            super(view);

            headerLine = view.findViewById(R.id.header_line);
            header = view.findViewById(R.id.header);
            dump = view.findViewById(R.id.dump);
            dumpBox = view.findViewById(R.id.dump_box);
            expandButton = view.findViewById(R.id.expand_button);
            contentType = view.findViewById(R.id.content_type);
        }
    }

    @NonNull
    @Override
    public PayloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.payload_item, parent, false);
        PayloadViewHolder holder = new PayloadViewHolder(view);

        holder.expandButton.setOnClickListener(v -> {
            int pos = holder.getAbsoluteAdapterPosition();
            Page page = getItem(pos);

            if(page.adaptChunk.isExpanded()) {
                int numPages = page.adaptChunk.getNumPages();
                int firstPagePos = pos - (numPages - 1);
                page.adaptChunk.collapse();
                notifyItemChanged(firstPagePos);
                notifyItemRangeRemoved(firstPagePos + 1, numPages - 1);
            } else {
                page.adaptChunk.expand();
                notifyItemChanged(pos);
                notifyItemRangeInserted(pos + 1, page.adaptChunk.getNumPages() - 1);
            }
        });

        return holder;
    }

    private String getHeaderTag(PayloadChunk chunk) {
        if(mMode == ChunkType.HTTP)
            return (chunk.is_sent) ? mContext.getString(R.string.request) : mContext.getString(R.string.response);
        else
            return chunk.is_sent ? mContext.getString(R.string.tx_direction) : mContext.getString(R.string.rx_direction);
    }

    @Override
    public void onBindViewHolder(@NonNull PayloadViewHolder holder, int position) {
        Page page = getItem(position);
        PayloadChunk chunk = page.adaptChunk.getPayloadChunk();

        if(page.isFirst()) {
            holder.headerLine.setVisibility(View.VISIBLE);

            Locale locale = Utils.getPrimaryLocale(mContext);
            holder.header.setText(String.format(locale,
                    "#%d [%s] %s — %s", page.adaptChunk.incrId + 1,
                    getHeaderTag(chunk),
                    (new SimpleDateFormat("HH:mm:ss.SSS", locale)).format(new Date(chunk.timestamp)),
                    Utils.formatBytes(chunk.payload.length)));

            holder.contentType.setText((chunk.contentType != null) ? chunk.contentType : "");
        } else
            holder.headerLine.setVisibility(View.GONE);

        if(page.isLast && page.adaptChunk.canBeExpanded()) {
            holder.expandButton.setVisibility(View.VISIBLE);
            holder.expandButton.setRotation(page.adaptChunk.isExpanded() ? 180 : 0);
        } else
            holder.expandButton.setVisibility(View.GONE);

        holder.dump.setText(page.getText());

        if(chunk.is_sent) {
            holder.dumpBox.setBackgroundResource(R.color.sentPayloadBg);
            holder.dump.setTextColor(ContextCompat.getColor(mContext, R.color.sentPayloadFg));
        } else {
            holder.dumpBox.setBackgroundResource(R.color.rcvdPayloadBg);
            holder.dump.setTextColor(ContextCompat.getColor(mContext, R.color.rcvdPayloadFg));
        }
    }

    @Override
    public int getItemCount() {
        // TODO remove loop, as it can generate ANRs on high number of elements
        int count = 0;

        for(AdapterChunk aChunk: mChunks)
            count += aChunk.getNumPages();

        return count;
    }

    public Page getItem(int pos) {
        if(pos < 0)
            return null;

        int count = 0;
        int i;

        // Find the AdapterChunk for the given page pos
        for(i=0; i < mChunks.size(); i++) {
            AdapterChunk aChunk = mChunks.get(i);
            int new_count = count + aChunk.getNumPages();

            if((pos >= count) && (pos < new_count))
                break;

            count = new_count;
        }

        if(i >= mChunks.size())
            return null;

        int pageIdx = pos - count;
        return mChunks.get(i).getPage(pageIdx);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setDisplayAsPrintableText(boolean asText) {
        if(mShowAsPrintable != asText) {
            mShowAsPrintable = asText;

            // Chunk pagination depends on the displayed data length, collapsing everything is simpler
            // than handling individual changes
            for(AdapterChunk chunk: mChunks)
                chunk.collapse(); // resets the chunk text
            notifyDataSetChanged();
        }
    }

    private int getAdapterPosition(AdapterChunk chunk) {
        int i;
        int count = 0;

        for(i=0; i < mChunks.size(); i++) {
            AdapterChunk aChunk = mChunks.get(i);
            if(aChunk == chunk)
                break;

            count += aChunk.getNumPages();
        }

        return count;
    }

    public void handleChunksAdded(int tot_chunks) {
        int items_count = -1;

        for(int i = mHandledChunks; i<tot_chunks; i++) {
            PayloadChunk chunk = mConn.getPayloadChunk(i);
            if(chunk == null)
                continue;

            // Exclude unrelated chunks
            if((mMode != ChunkType.RAW) && (mMode != chunk.type))
                continue;

            if(mMode == ChunkType.HTTP) {
                // will call onChunkReassembled
                if(chunk.is_sent)
                    mHttpReq.handleChunk(chunk);
                else
                    mHttpRes.handleChunk(chunk);
            } else {
                // TODO remove temporary caching due to slow getItemCount
                if(items_count == -1)
                    items_count = getItemCount();
                mChunks.add(new AdapterChunk(chunk, mChunks.size()));
                notifyItemInserted(items_count);
                items_count += 1;
            }
        }

        mHandledChunks = tot_chunks;
    }

    private void setNextUnrepliedRequest(int prevChunkIdx) {
        // Possibly find next un-replied HTTP request
        for(int i=prevChunkIdx + 1; i<mChunks.size(); i++) {
            AdapterChunk cur = mChunks.get(i);

            if(cur.mChunk.is_sent) {
                mUnrepliedHttpReq = cur;
                return;
            }
        }

        // no unreplied found
        mUnrepliedHttpReq = null;
    }

    @Override
    public void onChunkReassembled(PayloadChunk chunk) {
        AdapterChunk adapterChunk = new AdapterChunk(chunk, mChunks.size());
        int adapterPos = getItemCount();
        int insertPos = mChunks.size();

        // Need to determine where to add the chunk. If HTTP request, always add it to the bottom.
        // If HTTP reply, it should be added right after the first un-replied HTTP request
        if(!chunk.is_sent && (mUnrepliedHttpReq != null)) {
            // HTTP reply to a matching request
            int reqPos = mChunks.indexOf(mUnrepliedHttpReq);
            assert(reqPos >= 0);

            insertPos = reqPos + 1;
            adapterPos = getAdapterPosition(mUnrepliedHttpReq) + mUnrepliedHttpReq.getNumPages();
            Log.d(TAG, String.format("chunk #%d reply of #%d at %d", adapterChunk.incrId, mUnrepliedHttpReq.incrId, insertPos));
            setNextUnrepliedRequest(reqPos);
        } else if((chunk.is_sent) && (mUnrepliedHttpReq == null))
            mUnrepliedHttpReq = adapterChunk;

        mChunks.add(insertPos, adapterChunk);
        notifyItemInserted(adapterPos);
    }
}
