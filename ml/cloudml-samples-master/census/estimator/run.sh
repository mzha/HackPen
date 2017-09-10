# Run this from /census/estimator

gcloud ml-engine local train \
--module-name trainer.task \
--package-path trainer/ \
-- \
--train-files $TRAIN_DATA \
--eval-files $EVAL_DATA \
--train-steps 1000 \
--job-dir $MODEL_DIR \
--eval-steps 100
