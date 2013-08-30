/*
 *  Copyright (c) 2012-2013 DataTorrent, Inc.
 *  All Rights Reserved.
 */
package com.datatorrent.stram.engine;

import com.datatorrent.api.CheckpointListener;
import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.api.InputOperator;
import com.datatorrent.bufferserver.util.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chetan Narsude <chetan@datatorrent.com>
 */
public class RecoverableInputOperator implements InputOperator, CheckpointListener
{
  public final transient DefaultOutputPort<Long> output = new DefaultOutputPort<Long>();
  long checkpointedWindowId;
  boolean firstRun = true;
  transient boolean first;
  transient long windowId;
  int maximumTuples = 20;

  public void setMaximumTuples(int count)
  {
    maximumTuples = count;
  }

  @Override
  public void emitTuples()
  {
    if (first) {
      logger.debug("emitting {}", Codec.getStringWindowId(windowId));
      output.emit(windowId);
      first = false;
      if (--maximumTuples == 0) {
        throw new RuntimeException(new InterruptedException("Done!"));
      }
    }
  }

  @Override
  public void beginWindow(long windowId)
  {
    this.windowId = windowId;
    first = true;
  }

  @Override
  public void endWindow()
  {
  }

  @Override
  public void setup(OperatorContext context)
  {
    firstRun &= checkpointedWindowId == 0;
  }

  @Override
  public void teardown()
  {
  }

  @Override
  public void checkpointed(long windowId)
  {
    if (checkpointedWindowId == 0) {
      checkpointedWindowId = windowId;
    }

    logger.debug("{} checkpointed at {}", this, Codec.getStringWindowId(windowId));
  }

  @Override
  public void committed(long windowId)
  {
    logger.debug("{} committed at {}", this, Codec.getStringWindowId(windowId));

    if (firstRun && checkpointedWindowId > 0 && windowId > checkpointedWindowId) {
      throw new RuntimeException("Failure Simulation from " + this);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(RecoverableInputOperator.class);
}